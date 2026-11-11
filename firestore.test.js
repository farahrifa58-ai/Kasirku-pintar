const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DB_ID = "ai-studio-android-kasirpro-eda4af4b-cd58-4407-8dd7-d35c9009fbd8";

let testEnv;

before(async () => {
  const rules = fs.readFileSync("firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: rules,
      host: "127.0.0.1",
      port: 8085
    }
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

test("Unauthenticated user cannot create store", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("stores").doc("store123").set({
      storeId: "store123",
      ownerUid: "user1",
      storeName: "Toko Berkah",
      createdAt: new Date()
    })
  );
});

test("Store owner can create store", async () => {
  const ownerDb = testEnv.authenticatedContext("user_owner").firestore();
  await assertSucceeds(
    ownerDb.collection("stores").doc("store123").set({
      storeId: "store123",
      ownerUid: "user_owner",
      storeName: "Toko Berkah",
      cashierUids: ["user_cashier"],
      createdAt: new Date()
    })
  );
});

test("Owner can create and manage products", async () => {
  // Pre-seed store
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("stores").doc("store123").set({
      storeId: "store123",
      ownerUid: "user_owner",
      storeName: "Toko Berkah",
      cashierUids: ["user_cashier"]
    });
  });

  const ownerDb = testEnv.authenticatedContext("user_owner").firestore();
  await assertSucceeds(
    ownerDb.collection("stores").doc("store123").collection("products").doc("prod1").set({
      id: "prod1",
      storeId: "store123",
      name: "Beras Premium",
      category: "Sembako",
      unit: "kg",
      buyPrice: 12000,
      sellPrice: 15000,
      stock: 50,
      createdAt: new Date()
    })
  );
});

test("Cashier can update stock during sale, but cannot change product name/price", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("stores").doc("store123").set({
      storeId: "store123",
      ownerUid: "user_owner",
      storeName: "Toko Berkah",
      cashierUids: ["user_cashier"]
    });
    await context.firestore().collection("stores").doc("store123").collection("products").doc("prod1").set({
      id: "prod1",
      storeId: "store123",
      name: "Beras Premium",
      category: "Sembako",
      unit: "kg",
      buyPrice: 12000,
      sellPrice: 15000,
      stock: 50,
      createdAt: new Date()
    });
  });

  const cashierDb = testEnv.authenticatedContext("user_cashier").firestore();
  
  // Updating only stock: allowed
  await assertSucceeds(
    cashierDb.collection("stores").doc("store123").collection("products").doc("prod1").update({
      stock: 48,
      updatedAt: new Date()
    })
  );

  // Updating product name or price: forbidden for cashier
  await assertFails(
    cashierDb.collection("stores").doc("store123").collection("products").doc("prod1").update({
      name: "Nama Berubah",
      updatedAt: new Date()
    })
  );
});

test("Both owner and cashier can record transaction", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("stores").doc("store123").set({
      storeId: "store123",
      ownerUid: "user_owner",
      storeName: "Toko Berkah",
      cashierUids: ["user_cashier"]
    });
  });

  const cashierDb = testEnv.authenticatedContext("user_cashier").firestore();
  await assertSucceeds(
    cashierDb.collection("stores").doc("store123").collection("transactions").doc("trx1").set({
      id: "trx1",
      storeId: "store123",
      timestamp: Date.now(),
      itemsSummary: "Beras 2.0 kg",
      totalItems: 2,
      subtotal: 30000,
      discount: 0,
      tax: 0,
      total: 30000,
      totalProfit: 6000,
      paymentMethod: "Tunai",
      cashPaid: 50000,
      change: 20000,
      customerName: "Budi",
      cashierName: "Kasir 1",
      cashierUid: "user_cashier",
      createdAt: new Date()
    })
  );

  // Cashier CANNOT delete transaction
  await assertFails(
    cashierDb.collection("stores").doc("store123").collection("transactions").doc("trx1").delete()
  );

  // Owner CAN delete transaction
  const ownerDb = testEnv.authenticatedContext("user_owner").firestore();
  await assertSucceeds(
    ownerDb.collection("stores").doc("store123").collection("transactions").doc("trx1").delete()
  );
});

test("Non-member cannot read or write store data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("stores").doc("store123").set({
      storeId: "store123",
      ownerUid: "user_owner",
      storeName: "Toko Berkah",
      cashierUids: ["user_cashier"]
    });
  });

  const strangerDb = testEnv.authenticatedContext("user_stranger").firestore();
  await assertFails(
    strangerDb.collection("stores").doc("store123").collection("products").get()
  );
  await assertFails(
    strangerDb.collection("stores").doc("store123").collection("transactions").get()
  );
});
