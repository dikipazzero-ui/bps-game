const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_guard_1";
const BOB_UID = "bob_guard_2";
const CHARLIE_UID = "charlie_outsider_3";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserPayload(uid, now) {
  return {
    userId: uid,
    displayName: "Sgt. Alice",
    points: 250,
    apprehensions: 2,
    equippedUniformId: "rookie_cadet",
    unlockedUniformIds: ["rookie_cadet"],
    voiceEnabled: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validRoomPayload(hostUid, members, now) {
  return {
    roomCode: "CREST1",
    title: "Night Shift - Emerald Crest",
    hostUid,
    status: "ACTIVE",
    memberUids: members,
    northGateStatus: "OPEN",
    southGateStatus: "OPEN",
    scoreTotal: 0,
    apprehensionCount: 0,
    activeThiefSectorId: "BLOCK_B",
    activeThiefStreet: "Oakridge Boulevard",
    activeThiefX: 0.72,
    activeThiefY: 0.28,
    thiefSpotted: true,
    gandiLastLine: "Heads up! Movement near Cedar Court Villas!",
    gandiMood: "ALERT",
    createdAt: now,
    updatedAt: now,
  };
}

function validPlayerPayload(uid, now) {
  return {
    userId: uid,
    displayName: "Sgt. Alice",
    role: "GUARD",
    uniformId: "rookie_cadet",
    sectorId: "GATE_NORTH",
    posX: 0.5,
    posY: 0.12,
    pttActive: false,
    roomScore: 0,
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, now)));
  await assertFails(unauth.collection("rooms").doc("CREST1").get());
  await assertFails(unauth.collection("rooms").where("memberUids", "array-contains", ALICE_UID).get());
});

test("Users collection: owner create, partial update, cross-user denial, and ghost field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, now))
  );

  // Partial update: buy uniform & earn points
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      points: 350,
      apprehensions: 3,
      unlockedUniformIds: ["rookie_cadet", "neon_warden"],
      equippedUniformId: "neon_warden",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject ghost field / @DocumentId 'id' leakage
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserPayload(BOB_UID, now),
      id: BOB_UID,
    })
  );

  // Cross-user read & write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ points: 999999 }));
});

test("Rooms collection: filtered query alignment, join/leave, gate & thief state updates, terminal locking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("rooms").doc("CREST1").set(validRoomPayload(ALICE_UID, [ALICE_UID], created))
  );

  // Filtered memberUids query succeeds; unfiltered query fails
  await assertSucceeds(
    aliceDb.collection("rooms").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("rooms").get());

  // Bob joins by room code
  await assertSucceeds(
    bobDb.collection("rooms").doc("CREST1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 3000),
    })
  );

  // Bob (now a member) updates gate status & GANDI line
  await assertSucceeds(
    bobDb.collection("rooms").doc("CREST1").update({
      northGateStatus: "LOCKED",
      gandiLastLine: "North Main Gate LOCKED! Good hustle!",
      gandiMood: "PROUD",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Non-member Charlie cannot update room gameplay state
  await assertFails(
    charlieDb.collection("rooms").doc("CREST1").update({
      northGateStatus: "OPEN",
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  // Host ends room -> terminal state prevents further edits
  await assertSucceeds(
    aliceDb.collection("rooms").doc("CREST1").update({
      status: "ENDED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("rooms").doc("CREST1").update({
      southGateStatus: "LOCKED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Room subcollections (players, pings, messages): member-gated reads/writes and outsider denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("rooms").doc("CREST1").set(validRoomPayload(ALICE_UID, [ALICE_UID, BOB_UID], now))
  );

  // Alice creates her own player token
  await assertSucceeds(
    aliceDb.collection("rooms").doc("CREST1").collection("players").doc(ALICE_UID).set(
      validPlayerPayload(ALICE_UID, now)
    )
  );

  // Bob cannot overwrite Alice's player token
  await assertFails(
    bobDb.collection("rooms").doc("CREST1").collection("players").doc(ALICE_UID).update({
      posX: 0.9,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob can drop a tactical ping and send a squad message
  await assertSucceeds(
    bobDb.collection("rooms").doc("CREST1").collection("pings").doc("ping_1").set({
      senderId: BOB_UID,
      senderName: "Unit Bob",
      pingType: "BACKUP",
      sectorId: "CROSSWALK",
      streetName: "Central Crosswalk",
      posX: 0.5,
      posY: 0.5,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    bobDb.collection("rooms").doc("CREST1").collection("messages").doc("msg_1").set({
      senderId: BOB_UID,
      senderName: "Unit Bob",
      channel: "RADIO",
      text: "Corner North Gate!",
      sectorTag: "GATE_NORTH",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Messages & pings are immutable (update denied)
  await assertFails(
    bobDb.collection("rooms").doc("CREST1").collection("messages").doc("msg_1").update({
      text: "Tampered",
    })
  );

  // Outsider Charlie cannot list players, pings, or messages
  await assertFails(
    charlieDb.collection("rooms").doc("CREST1").collection("players").get()
  );
  await assertFails(
    charlieDb.collection("rooms").doc("CREST1").collection("pings").get()
  );
  await assertFails(
    charlieDb.collection("rooms").doc("CREST1").collection("messages").get()
  );
});
