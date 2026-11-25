const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_farmer_123";
const BOB_UID = "bob_farmer_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user cannot read or write farmer profiles or saved lots", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .set({
        userId: ALICE_UID,
        displayName: "Unauth",
        preferredLanguage: "en",
        selectedCropId: "sugarcane",
        preferredVehicleRate: 22,
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("saved_lots").get()
  );
});

test("Authenticated owner can create and partially update FarmerProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Ramesh Gowda",
      preferredLanguage: "kn",
      selectedCropId: "sugarcane",
      preferredVehicleRate: 22,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update of mutable fields only
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      selectedCropId: "arecanut",
      preferredLanguage: "en",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Ramesh",
      preferredLanguage: "en",
      selectedCropId: "paddy",
      preferredVehicleRate: 15,
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Authenticated owner can create, list, update, and delete saved_lots without @DocumentId id field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const lotsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("saved_lots");

  await assertSucceeds(
    lotsCol.doc("lot_sugarcane_1").set({
      userId: ALICE_UID,
      cropId: "sugarcane",
      cropName: "Sugarcane",
      mandiName: "Mandya Sugar Yard",
      quantityQuintals: 100,
      modalPricePerQuintal: 3450,
      netProfit: 344120,
      transportCost: 880,
      verdict: "HOLD_WAIT",
      notes: "2026 harvest lot",
      createdAt: created,
      updatedAt: created,
    })
  );

  // List matches Kotlin observeSavedLots() on empty or populated subcollection
  await assertSucceeds(lotsCol.get());

  // Partial update on allowed mutable keys
  await assertSucceeds(
    lotsCol.doc("lot_sugarcane_1").update({
      notes: "Booked tractor for morning",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject unexpected @DocumentId 'id' field in payload
  await assertFails(
    lotsCol.doc("lot_bad_id").set({
      id: "lot_bad_id",
      userId: ALICE_UID,
      cropId: "wheat",
      cropName: "Wheat",
      mandiName: "Indore Grain Mandi",
      quantityQuintals: 25,
      modalPricePerQuintal: 2680,
      netProfit: 66000,
      transportCost: 1000,
      verdict: "SELL_NOW",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future tampered timestamp
  await assertFails(
    lotsCol.doc("lot_future").set({
      userId: ALICE_UID,
      cropId: "wheat",
      cropName: "Wheat",
      mandiName: "Indore Grain Mandi",
      quantityQuintals: 25,
      modalPricePerQuintal: 2680,
      netProfit: 66000,
      transportCost: 1000,
      verdict: "SELL_NOW",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(lotsCol.doc("lot_sugarcane_1").delete());
});

test("Cross-user isolation prevents Bob from reading or modifying Alice's profile or saved lots", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    const ts = new Date(Date.now() - 5000);
    await adminDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Farmer",
      preferredLanguage: "en",
      selectedCropId: "sugarcane",
      preferredVehicleRate: 22,
      createdAt: ts,
      updatedAt: ts,
    });
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_lots")
      .doc("lot_1")
      .set({
        userId: ALICE_UID,
        cropId: "sugarcane",
        cropName: "Sugarcane",
        mandiName: "Mandya Sugar Yard",
        quantityQuintals: 80,
        modalPricePerQuintal: 3450,
        netProfit: 275000,
        transportCost: 1000,
        verdict: "SELL_NOW",
        createdAt: ts,
        updatedAt: ts,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .update({ displayName: "Hijacked", updatedAt: new Date(Date.now() - 500) })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_lots").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_lots")
      .doc("lot_1")
      .delete()
  );
});
