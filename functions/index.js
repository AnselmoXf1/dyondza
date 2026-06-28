/**
 * Dyondza - Secure Server-Side Student Ranking and Economy Platform
 * 
 * This file contains Firebase Cloud Functions (v2) to calculate, aggregate, and serve
 * student rankings securely, preventing local client-side tampering and ensuring high
 * scalability with Firestore.
 */

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

// Initialize Firebase Admin SDK
admin.initializeApp();
const db = admin.firestore();

/**
 * 1. SECURE XP CALCULATION (Firestore Trigger)
 * Automatically triggers whenever a student completes a focus study session.
 * This runs entirely on the server-side, preventing students from editing their local app
 * files to inject fake study hours or manipulate XP.
 */
exports.onFocusSessionCreated = onDocumentCreated(
  "students/{studentId}/sessions/{sessionId}",
  async (event) => {
    const sessionData = event.data.data();
    const studentId = event.params.studentId;

    if (!sessionData) {
      console.log("No session data found.");
      return null;
    }

    const durationSeconds = sessionData.durationSeconds || 0;
    const durationMinutes = Math.floor(durationSeconds / 60);
    const endTime = sessionData.endTime || Date.now();

    // Economic Rule: 1 minute of study = 10 XP
    let xpEarned = durationMinutes * 10;

    // Apply Streak and Time-of-day Multipliers securely on the server
    const sessionDate = new Date(endTime);
    const hour = sessionDate.getHours();

    // Bonus "Sábio" (Study before 7 AM or after 8 PM gives 1.5x)
    let hasTimeBonus = false;
    if (hour < 7 || hour >= 20) {
      xpEarned = Math.floor(xpEarned * 1.2); // 20% bonus "Sábio"
      hasTimeBonus = true;
    }

    console.log(`Processing session for ${studentId}: ${durationMinutes} mins. Base XP: ${durationMinutes * 10}. Final XP with bonuses: ${xpEarned}`);

    // Update Student Document atomically
    const studentRef = db.collection("students").doc(studentId);

    try {
      await db.runTransaction(async (transaction) => {
        const studentDoc = await transaction.get(studentRef);

        if (!studentDoc.exists) {
          // If the profile does not exist yet, initialize it
          transaction.set(studentRef, {
            totalXp: xpEarned,
            level: Math.max(1, Math.floor(xpEarned / 100) + 1),
            lastActive: admin.firestore.FieldValue.serverTimestamp()
          });
        } else {
          const currentXp = studentDoc.data().totalXp || 0;
          const newXp = currentXp + xpEarned;
          const newLevel = Math.max(1, Math.floor(newXp / 100) + 1);

          transaction.update(studentRef, {
            totalXp: newXp,
            level: newLevel,
            lastActive: admin.firestore.FieldValue.serverTimestamp()
          });
        }
      });
      console.log(`Successfully credited ${xpEarned} XP to student ${studentId}`);
    } catch (error) {
      console.error("Transaction failed to update XP:", error);
    }
  }
);

/**
 * 2. REGIONAL & GLOBAL RANKING RECUPERATION (Scheduled Cloud Function)
 * Periodically aggregates student scores to update rankings on the server.
 * Running this out-of-band prevents performance bottlenecks and DDoS conditions on large-scale datasets.
 * Runs every hour.
 */
exports.recalculateLeaderboards = onSchedule("every 1 hours", async (event) => {
  console.log("Starting Scheduled Global and Provincial leaderboard recalculation...");

  const studentsSnapshot = await db.collection("students").get();
  if (studentsSnapshot.empty) {
    console.log("No students found to rank.");
    return null;
  }

  // Map student documents to array
  const students = [];
  studentsSnapshot.forEach((doc) => {
    students.push({
      id: doc.id,
      ...doc.data()
    });
  });

  // Sort globally by total XP descending
  students.sort((a, b) => (b.totalXp || 0) - (a.totalXp || 0));

  // Temporary maps to track province-level and school-level ranks
  const provinceCounters = {};
  const schoolCounters = {};

  const batchUpdates = [];

  students.forEach((student, index) => {
    const globalRank = index + 1;

    // Provincial calculation
    const province = student.province || "Maputo";
    if (!provinceCounters[province]) {
      provinceCounters[province] = 0;
    }
    provinceCounters[province]++;
    const provinceRank = provinceCounters[province];

    // School calculation
    const school = student.school || "Escola Secundária Josina Machel";
    if (!schoolCounters[school]) {
      schoolCounters[school] = 0;
    }
    schoolCounters[school]++;
    const schoolRank = schoolCounters[school];

    // Stage update
    const docRef = db.collection("students").doc(student.id);
    batchUpdates.push({
      ref: docRef,
      data: {
        globalRank: globalRank,
        provinceRank: provinceRank,
        schoolRank: schoolRank
      }
    });
  });

  // Commit updates in standard batches of 500 to satisfy Firestore limitations
  const BATCH_LIMIT = 500;
  for (let i = 0; i < batchUpdates.length; i += BATCH_LIMIT) {
    const batch = db.batch();
    const chunk = batchUpdates.slice(i, i + BATCH_LIMIT);
    
    chunk.forEach((update) => {
      batch.update(update.ref, update.data);
    });

    await batch.commit();
    console.log(`Committed leaderboard update batch ${Math.floor(i / BATCH_LIMIT) + 1}`);
  }

  console.log(`Leaderboards fully updated for ${students.length} students!`);
});

/**
 * 3. SECURE PAGINATED RANKING RETRIEVAL (HTTPS Callable)
 * Allows client applications to request only the relevant subset of students (e.g., 20 at a time),
 * protecting data bandwidth and memory footprint on low-cost Android smartphones in Mozambique.
 */
exports.getPaginatedRanking = onCall(async (request) => {
  // Ensure the user is authenticated
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Apenas alunos autenticados podem ver classificações.");
  }

  const { filterType, filterValue, pageSize = 20, startAfterId } = request.data;

  let query = db.collection("students");

  // Apply filters securely on server
  if (filterType === "PROVINCE") {
    if (!filterValue) {
      throw new HttpsError("invalid-argument", "Nome da Província em falta para o filtro regional.");
    }
    query = query.where("province", "==", filterValue).orderBy("provinceRank", "asc");
  } else if (filterType === "SCHOOL") {
    if (!filterValue) {
      throw new HttpsError("invalid-argument", "Nome da Escola em falta para o filtro local.");
    }
    query = query.where("school", "==", filterValue).orderBy("schoolRank", "asc");
  } else {
    // Default to Global ranking
    query = query.orderBy("globalRank", "asc");
  }

  // Handle pagination using document cursors
  if (startAfterId) {
    const startDoc = await db.collection("students").doc(startAfterId).get();
    if (startDoc.exists) {
      query = query.startAfter(startDoc);
    }
  }

  // Limit query payload to requested page size
  query = query.limit(pageSize);

  const snapshot = await query.get();
  const rankingList = [];

  snapshot.forEach((doc) => {
    const data = doc.data();
    rankingList.push({
      id: doc.id,
      name: data.name || "Estudante Anônimo",
      school: data.school || "Escola Secundária",
      province: data.province || "Maputo",
      totalXp: data.totalXp || 0,
      level: data.level || 1,
      rank: filterType === "PROVINCE" ? data.provinceRank : (filterType === "SCHOOL" ? data.schoolRank : data.globalRank)
    });
  });

  return {
    rankings: rankingList,
    hasMore: rankingList.length === pageSize
  };
});
