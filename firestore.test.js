const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const CEO_UID = "ceo_gideon_uid";
const CEO_EMAIL = "agchu30@gmail.com";
const SELLER_UID = "seller_pauline_uid";
const BUYER_UID = "buyer_marc_uid";
const ATTACKER_UID = "attacker_uid";

const SERVER_TS = new Date();

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

function ceoDb() {
  return testEnv.authenticatedContext(CEO_UID, {
    email: CEO_EMAIL,
    email_verified: true,
  }).firestore();
}

function spoofedCeoDb() {
  return testEnv.authenticatedContext(ATTACKER_UID, {
    email: CEO_EMAIL,
    email_verified: false,
  }).firestore();
}

function sellerDb() {
  return testEnv.authenticatedContext(SELLER_UID, {
    email: "pauline@seller.cm",
    email_verified: true,
  }).firestore();
}

function buyerDb() {
  return testEnv.authenticatedContext(BUYER_UID, {
    email: "marc@buyer.cm",
    email_verified: true,
  }).firestore();
}

function baseListing(overrides = {}) {
  return {
    sellerUid: SELLER_UID,
    sellerName: "Pauline Etoga",
    sellerEmail: "pauline@seller.cm",
    sellerPhone: "+237677112233",
    title: "500 m2 Titled Land in Kribi",
    description: "Surveyed residential plot with paved road access.",
    category: "LAND",
    priceXaf: 18000000,
    location: "Kribi",
    specs: "500 m2 - Titre Foncier",
    imageUri: "drawable://img_land_estate",
    isOwnerListing: false,
    approvalStatus: "PENDING_APPROVAL",
    paymentStatus: "PAYMENT_SUBMITTED",
    paymentMethod: "MTN Mobile Money (+237 676 831 188)",
    paymentReference: "MP261119.0930.A44120",
    payerPhone: "+237677112233",
    listingFeeXaf: 15000,
    commissionRatePercent: 5,
    adminNotes: "",
    createdAt: SERVER_TS,
    updatedAt: SERVER_TS,
    ...overrides,
  };
}

test("Unauthenticated caller cannot read users, listings, or escrow_transactions", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(SELLER_UID).get());
  await assertFails(unauth.collection("listings").where("approvalStatus", "==", "APPROVED").get());
  await assertFails(unauth.collection("escrow_transactions").get());
});

test("Seller registration succeeds for USER role, blocks OWNER_ADMIN self-escalation, and blocks cross-user PII read", async () => {
  const sDb = sellerDb();
  await assertSucceeds(
    sDb.collection("users").doc(SELLER_UID).set({
      userId: SELLER_UID,
      fullName: "Pauline Etoga",
      email: "pauline@seller.cm",
      phone: "+237677112233",
      role: "USER",
      createdAt: SERVER_TS,
      updatedAt: SERVER_TS,
    })
  );

  await assertFails(
    buyerDb().collection("users").doc(BUYER_UID).set({
      userId: BUYER_UID,
      fullName: "Fake Admin",
      email: "marc@buyer.cm",
      phone: "+237690000000",
      role: "OWNER_ADMIN",
      createdAt: SERVER_TS,
      updatedAt: SERVER_TS,
    })
  );

  // Buyer cannot read Seller's PII document
  await assertFails(buyerDb().collection("users").doc(SELLER_UID).get());
  // CEO agchu30@gmail.com CAN read Seller profile
  await assertSucceeds(ceoDb().collection("users").doc(SELLER_UID).get());
});

test("Email spoofing with unverified email_verified=false is rejected for CEO admin operations", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("listings").doc("listing_1").set(baseListing());
  });

  await assertFails(
    spoofedCeoDb().collection("listings").doc("listing_1").update({
      approvalStatus: "APPROVED",
      paymentStatus: "PAYMENT_VERIFIED",
      adminNotes: "Spoofed",
      updatedAt: SERVER_TS,
    })
  );
});

test("Seller cannot self-approve advert; only CEO agchu30@gmail.com can approve after MTN MoMo payment", async () => {
  const sDb = sellerDb();
  // Seller cannot create pre-approved listing
  await assertFails(
    sDb.collection("listings").doc("listing_bad").set(
      baseListing({ approvalStatus: "APPROVED", isOwnerListing: true })
    )
  );

  // Seller creates pending listing with MTN MoMo ref
  await assertSucceeds(sDb.collection("listings").doc("listing_land").set(baseListing()));

  // Seller cannot approve their own listing
  await assertFails(
    sDb.collection("listings").doc("listing_land").update({
      approvalStatus: "APPROVED",
      paymentStatus: "PAYMENT_VERIFIED",
      updatedAt: SERVER_TS,
    })
  );

  // Buyer cannot list pending adverts without filter matching APPROVED
  await assertFails(buyerDb().collection("listings").get());
  await assertFails(buyerDb().collection("listings").doc("listing_land").get());

  // CEO agchu30@gmail.com verifies payment & approves advert
  await assertSucceeds(
    ceoDb().collection("listings").doc("listing_land").update({
      approvalStatus: "APPROVED",
      paymentStatus: "PAYMENT_VERIFIED",
      adminNotes: "Payment verified & approved by CEO LAGCHU LAGCHU GIDEON",
      updatedAt: SERVER_TS,
    })
  );

  // Now Buyer can query approved listings and read listing_land
  await assertSucceeds(
    buyerDb().collection("listings").where("approvalStatus", "==", "APPROVED").get()
  );
  await assertSucceeds(buyerDb().collection("listings").doc("listing_land").get());
});

test("Escrow transaction enforces 5% partner shop commission math and buyer/admin isolation", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("listings").doc("listing_prod").set(
      baseListing({
        category: "ELECTRONICS",
        title: "Solar Inverter 5kVA",
        priceXaf: 1000000,
        approvalStatus: "APPROVED",
        paymentStatus: "PAYMENT_VERIFIED",
        commissionRatePercent: 5,
      })
    );
  });

  // Invalid commission math (sum != amountXaf) must fail
  await assertFails(
    buyerDb().collection("escrow_transactions").doc("tx_bad").set({
      referenceCode: "GGM-112233",
      listingId: "listing_prod",
      listingTitle: "Solar Inverter 5kVA",
      listingCategory: "ELECTRONICS",
      amountXaf: 1000000,
      commissionRatePercent: 5,
      commissionXaf: 10000,
      netSellerPayoutXaf: 950000,
      buyerUid: BUYER_UID,
      buyerName: "Marc Talla",
      buyerEmail: "marc@buyer.cm",
      buyerPhone: "+237690112233",
      sellerUid: SELLER_UID,
      sellerName: "Pauline Etoga",
      sellerPhone: "+237677112233",
      paymentChannel: "MTN Mobile Money (+237 676 831 188)",
      buyerPaymentRef: "MP261119.1200.B99110",
      escrowStatus: "ESCROW_PENDING_VERIFY",
      deliveryNote: "Awaiting confirmation",
      createdAt: SERVER_TS,
      updatedAt: SERVER_TS,
    })
  );

  // Valid 5% commission (50,000 commission + 950,000 net = 1,000,000) succeeds
  await assertSucceeds(
    buyerDb().collection("escrow_transactions").doc("tx_valid").set({
      referenceCode: "GGM-884422",
      listingId: "listing_prod",
      listingTitle: "Solar Inverter 5kVA",
      listingCategory: "ELECTRONICS",
      amountXaf: 1000000,
      commissionRatePercent: 5,
      commissionXaf: 50000,
      netSellerPayoutXaf: 950000,
      buyerUid: BUYER_UID,
      buyerName: "Marc Talla",
      buyerEmail: "marc@buyer.cm",
      buyerPhone: "+237690112233",
      sellerUid: SELLER_UID,
      sellerName: "Pauline Etoga",
      sellerPhone: "+237677112233",
      paymentChannel: "MTN Mobile Money (+237 676 831 188)",
      buyerPaymentRef: "MP261119.1200.B99110",
      escrowStatus: "ESCROW_PENDING_VERIFY",
      deliveryNote: "Awaiting confirmation",
      createdAt: SERVER_TS,
      updatedAt: SERVER_TS,
    })
  );

  // Unrelated attacker cannot read buyer's escrow order
  await assertFails(spoofedCeoDb().collection("escrow_transactions").doc("tx_valid").get());
  // CEO agchu30@gmail.com CAN confirm & complete escrow
  await assertSucceeds(
    ceoDb().collection("escrow_transactions").doc("tx_valid").update({
      escrowStatus: "COMPLETED_RELEASED",
      deliveryNote: "Released by CEO LAGCHU LAGCHU GIDEON",
      updatedAt: SERVER_TS,
    })
  );
});
