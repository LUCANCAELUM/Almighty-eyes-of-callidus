const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_callidus_01";
const BOB_UID = "bob_callidus_02";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any Callidus collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("bookmarks").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("donations").doc("d1").set({
      userId: ALICE_UID,
      paybill: "522522",
      accountNumber: "1355643775",
      amountKes: 500,
      donorPhone: "0712345678",
      referenceCode: "MPESA123",
      status: "RECORDED",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated owner can create and update profile, bookmarks, donations, and chat messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const updated = new Date(Date.now() - 1000);

  // UserProfile create & partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Commander Alice",
      email: "alice@callidus.dev",
      opticMode: "EO_TACTICAL",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      opticMode: "FLIR_THERMAL",
      updatedAt: updated,
    })
  );

  // Bookmark create, list, delete (no @DocumentId `id` field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("sat_25544").set({
      userId: ALICE_UID,
      targetType: "SATELLITE",
      targetKey: "sat_25544",
      title: "ISS (ZARYA)",
      subtitle: "NORAD 25544",
      lat: 51.4,
      lon: -0.1,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());

  // Donation to Paybill 522522 Account 1355643775
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("donations").doc("don_01").set({
      userId: ALICE_UID,
      paybill: "522522",
      accountNumber: "1355643775",
      amountKes: 1000,
      donorPhone: "+254700000001",
      referenceCode: "SH12345XYZ",
      status: "RECORDED",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("donations").doc("don_01").update({
      status: "CONFIRMED",
      updatedAt: updated,
    })
  );

  // IntelChatMessage create & list
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("intel_messages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      content: "Analyze airspace over London.",
      modelId: "gemini-3.8-flash",
      targetLat: 51.47,
      targetLon: -0.45,
      createdAt: created,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("intel_messages").get());
});

test("Rejects cross-user access, tampered paybill/account, ghost fields, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("donations").doc("don_bob").set({
      userId: BOB_UID,
      paybill: "522522",
      accountNumber: "1355643775",
      amountKes: 500,
      donorPhone: "0711",
      referenceCode: "REF",
      status: "RECORDED",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Tampered Paybill or Account Number must be rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("donations").doc("bad_paybill").set({
      userId: ALICE_UID,
      paybill: "999999",
      accountNumber: "1355643775",
      amountKes: 500,
      donorPhone: "0711",
      referenceCode: "REF",
      status: "RECORDED",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Ghost field (`id` or `isAdmin`) must be rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("ghost_id").set({
      id: "ghost_id",
      userId: ALICE_UID,
      targetType: "FLIGHT",
      targetKey: "flight_1",
      title: "BAW123",
      subtitle: "London",
      lat: 51.5,
      lon: -0.1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Future timestamp must be rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("intel_messages").doc("future_msg").set({
      userId: ALICE_UID,
      role: "user",
      content: "Test",
      modelId: "gemini-3.8-flash",
      targetLat: 0,
      targetLon: 0,
      createdAt: new Date(Date.now() + 60000),
    })
  );
});
