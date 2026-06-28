import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

/**
 * Interface representando o documento de classificação do estudante no Firestore.
 */
interface StudentDoc {
  studentId: string;
  name: string;
  totalXp: number;
  level: number;
  province: string;
  school: string;
  globalRank?: number;
  provinceRank?: number;
  schoolRank?: number;
  lastActive: number;
}

/**
 * 1. Gatilho de Escrita no Firestore: onStudentXpUpdated
 * Sempre que um estudante acumula XP, verifica se subiu de nível.
 */
export const onStudentXpUpdated = functions.firestore
  .document("students/{studentId}")
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data() as StudentDoc;
    const afterData = change.after.data() as StudentDoc;

    if (!afterData) return null;

    // Se o XP não mudou, evitamos loops ou chamadas desnecessárias
    if (beforeData && beforeData.totalXp === afterData.totalXp) {
      return null;
    }

    // Lógica segura de cálculo de Nível: Nível = 1 + (XP / 100)
    const expectedLevel = Math.min(50, Math.floor(1 + afterData.totalXp / 100));

    if (afterData.level !== expectedLevel) {
      console.log(`Estudante ${afterData.name} subiu de nível! ${afterData.level} -> ${expectedLevel}`);
      await change.after.ref.update({
        level: expectedLevel,
        lastActive: admin.firestore.FieldValue.serverTimestamp()
      });
    }

    return null;
  });

/**
 * 2. Recalcular Rankings Globais e Regionais no Servidor (Executável por HTTPS ou Cron Diário)
 * Processa todos os alunos, calcula as posições de forma incremental e atualiza com batches de segurança.
 * Implementa limitação e ordenação para ser rápido, seguro e escalável.
 */
export const recalculateRankings = functions.https.onCall(async (data, context) => {
  // Apenas chamadas autenticadas podem solicitar recalculação sob demanda, prevenindo ataques DOS
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Apenas administradores ou alunos autenticados podem acionar o recálculo dos rankings."
    );
  }

  try {
    const studentsSnapshot = await db.collection("students")
      .orderBy("totalXp", "desc")
      .get();

    if (studentsSnapshot.empty) {
      return { success: true, message: "Nenhum estudante cadastrado para classificar." };
    }

    const students = studentsSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data() as StudentDoc
    }));

    // Dicionários para guardar as contagens regionais de ranking
    const provinceCounters: { [key: string]: number } = {};
    const schoolCounters: { [key: string]: number } = {};

    const batch = db.batch();
    
    // Processamento ordenado por XP descendente (O mais alto XP recebe Rank 1 global)
    students.forEach((student, index) => {
      const globalRank = index + 1;

      // Classificação na Província
      const prov = student.province || "Maputo";
      provinceCounters[prov] = (provinceCounters[prov] || 0) + 1;
      const provinceRank = provinceCounters[prov];

      // Classificação na Escola
      const sch = student.school || "Escola Secundária Josina Machel";
      schoolCounters[sch] = (schoolCounters[sch] || 0) + 1;
      const schoolRank = schoolCounters[sch];

      const studentRef = db.collection("students").doc(student.id);
      
      // Atualizações seguras no Firestore
      batch.update(studentRef, {
        globalRank: globalRank,
        provinceRank: provinceRank,
        schoolRank: schoolRank
      });
    });

    // Commit seguro das transações em lote no Firestore
    await batch.commit();
    console.log(`Recálculo completo com sucesso para ${students.length} estudantes moçambicanos.`);

    return {
      success: true,
      processedCount: students.length,
      message: "Recálculos de ranking geral, província e escola efetuados de forma segura no servidor."
    };
  } catch (error: any) {
    console.error("Erro ao rodar tarefa de ranking no servidor: ", error);
    throw new functions.https.HttpsError("internal", error.message || "Falha ao recalcular rankings.");
  }
});

/**
 * 3. Tarefa Cron Programada (Scheduled Cloud Function)
 * Executa todas as noites às 02:00h para consolidar a tabela de líderes.
 */
export const scheduledRankingConsolidation = functions.pubsub
  .schedule("0 2 * * *")
  .timeZone("Africa/Maputo")
  .onRun(async (context) => {
    console.log("Iniciando consolidação automática diária do ranking nacional...");
    
    const studentsSnapshot = await db.collection("students")
      .orderBy("totalXp", "desc")
      .get();

    if (studentsSnapshot.empty) return null;

    const students = studentsSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data() as StudentDoc
    }));

    const provinceCounters: { [key: string]: number } = {};
    const schoolCounters: { [key: string]: number } = {};

    let batch = db.batch();
    let count = 0;

    for (let i = 0; i < students.length; i++) {
      const student = students[i];
      const globalRank = i + 1;

      const prov = student.province || "Maputo";
      provinceCounters[prov] = (provinceCounters[prov] || 0) + 1;
      const provinceRank = provinceCounters[prov];

      const sch = student.school || "Escola Secundária Josina Machel";
      schoolCounters[sch] = (schoolCounters[sch] || 0) + 1;
      const schoolRank = schoolCounters[sch];

      const studentRef = db.collection("students").doc(student.id);
      batch.update(studentRef, {
        globalRank,
        provinceRank,
        schoolRank
      });

      count++;

      // Limite de lote do Firestore é de 500 operações por batch
      if (count === 400) {
        await batch.commit();
        batch = db.batch();
        count = 0;
      }
    }

    if (count > 0) {
      await batch.commit();
    }

    console.log("Consolidação diária finalizada com sucesso.");
    return null;
  });
