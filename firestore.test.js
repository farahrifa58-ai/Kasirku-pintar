const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";

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
    unauthDb.collection("stores").doc("store_user1").set({
      storeId: "store_user1",
      ownerUid: "user1",
      storeName: "Toko Berkah",
      createdAt: new Date()
    })
  );
});

test("Store owner can create and read store using UID pattern", async () => {
  const ownerDb = testEnv.authenticatedContext("user_owner").firestore();
  const storeId = "store_user_owner";
  
  await assertSucceeds(
    ownerDb.collection("stores").doc(storeId).set({
      storeId: storeId,
      ownerUid: "user_owner",
      ownerEmail: "owner@gmail.com",
      storeName: "Toko Berkah",
      ownerName: "Pemilik",
      address: "Jl. Merdeka No. 1",
      whatsapp: "08123456789",
      footerMessage: "Terima Kasih",
      taxEnabled: true,
      defaultTaxPercent: 11.0,
      paperSize: "58mm",
      cashierUids: [],
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );

  // Read should succeed for owner
  await assertSucceeds(
    ownerDb.collection("stores").doc(storeId).get()
  );
});

test("Store owner can create, read, and update products in store_uid", async () => {
  const ownerDb = testEnv.authenticatedContext("user_owner").firestore();
  const storeId = "store_user_owner";

  // Create product
  await assertSucceeds(
    ownerDb.collection("stores").doc(storeId).collection("products").doc("prod1").set({
      id: "prod1",
      storeId: storeId,
      name: "Beras Premium",
      category: "Sembako",
      unit: "kg",
      buyPrice: 12000,
      sellPrice: 15000,
      stock: 50,
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );

  // Read product
  await assertSucceeds(
    ownerDb.collection("stores").doc(storeId).collection("products").doc("prod1").get()
  );

  // Update stock during sale
  await assertSucceeds(
    ownerDb.collection("stores").doc(storeId).collection("products").doc("prod1").update({
      stock: 45,
      updatedAt: new Date()
    })
  );
});

test("Store owner can record transaction in store_uid", async () => {
  const ownerDb = testEnv.authenticatedContext("user_owner").firestore();
  const storeId = "store_user_owner";

  await assertSucceeds(
    ownerDb.collection("stores").doc(storeId).collection("transactions").doc("trx1").set({
      id: "trx1",
      storeId: storeId,
      timestamp: Date.now(),
      itemsSummary: "Beras Premium 5 kg",
      totalItems: 5,
      subtotal: 75000,
      discount: 0,
      tax: 0,
      total: 75000,
      totalProfit: 15000,
      paymentMethod: "Tunai",
      cashPaid: 100000,
      change: 25000,
      customerName: "Pelanggan",
      cashierName: "Kasir 1",
      cashierUid: "user_owner",
      notes: "",
      createdAt: new Date()
    })
  );
});

test("Cross-user access is denied: stranger cannot access owner's store or products", async () => {
  const ownerDb = testEnv.authenticatedContext("user_owner").firestore();
  const storeId = "store_user_owner";

  // Owner seeds store and product
  await ownerDb.collection("stores").doc(storeId).set({
    storeId: storeId,
    ownerUid: "user_owner",
    storeName: "Toko Berkah",
    createdAt: new Date()
  });
  await ownerDb.collection("stores").doc(storeId).collection("products").doc("prod1").set({
    id: "prod1",
    storeId: storeId,
    name: "Barang Rahasia",
    category: "Umum",
    unit: "pcs",
    buyPrice: 1000,
    sellPrice: 2000,
    stock: 10,
    createdAt: new Date()
  });

  const strangerDb = testEnv.authenticatedContext("user_stranger").firestore();

  // Stranger cannot read store
  await assertFails(
    strangerDb.collection("stores").doc(storeId).get()
  );

  // Stranger cannot read products
  await assertFails(
    strangerDb.collection("stores").doc(storeId).collection("products").get()
  );

  // Stranger cannot write to transactions
  await assertFails(
    strangerDb.collection("stores").doc(storeId).collection("transactions").doc("trx2").set({
      id: "trx2",
      storeId: storeId,
      timestamp: Date.now(),
      itemsSummary: "Item",
      total: 10000,
      paymentMethod: "Tunai",
      createdAt: new Date()
    })
  );
});
