// Import the native module. On web, it will be resolved to DynamicYield.web.ts
// and on native platforms to DynamicYield.ts
import { useFocusEffect } from "expo-router";
import { useCallback } from "react";

import { Platform } from "react-native";
import DynamicYieldModule from "./src/DynamicYieldModule";

const IS_IOS = Platform.OS === "ios";

// Types
export type DataCenter = "US" | "EU";
export type DeviceType = "SMARTPHONE" | "TABLET" | "KIOSK" | "ODMB";
export type Channel = "APP" | "KIOSK" | "DRIVE_THRU";
export type LogLevel = "VERBOSE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "NONE";
export type EngagementType = "CLICK" | "CONVERSION";
export type DayPart = "BREAKFAST" | "LUNCH" | "DINNER";
export type CuidType = "EMAIL" | "EXTERNAL";
export type VideoProgressType = "START" | "PROGRESS" | "COMPLETE";
export type SortOrderType = "ASC" | "DESC";

const apiKey = IS_IOS
  ? process.env.EXPO_PUBLIC_DY_API_KEY_IOS
  : process.env.EXPO_PUBLIC_DY_API_KEY_ANDROID;

interface InitializeOptions {
  apiKey: string;
  dataCenter: DataCenter;
  deviceType?: DeviceType;
  channel?: Channel;
  ip?: string;
  locale?: string;
  isImplicitPageview?: boolean;
  isImplicitImpressionMode?: boolean;
  customUrl?: string;
  sharedDevice?: boolean;
  deviceId?: string;
}

interface ChooseVariationsOptions {
  pageLocation: string;
  pageReferrer?: string;
  selectorNames?: string[];
  selectorGroups?: string[];
  selectorPreviews?: string[];
  dayPart?: DayPart;
  cart?: {
    productId: string;
    quantity: number;
    itemPrice: number;
    innerProducts?: {
      productId: string;
      quantity: number;
      itemPrice: number;
    }[];
  }[];
  branchId?: string;
  options?: {
    reportPageview?: boolean;
    reportImpression?: boolean;
    returnMetadata?: boolean;
  };
  pageAttributes?: Record<string, string | number>;
  recsProductData?: {
    fieldFilter?: string[];
    skusOnly?: boolean;
  };
}

interface CartItem {
  productId: string;
  quantity: number;
  itemPrice: number;
}

interface PurchaseEventOptions {
  eventName: string;
  value: number;
  cart: CartItem[];
  currency?: string;
  uniqueTransactionId?: string;
}

interface AddToCartEventOptions {
  eventName: string;
  value: number;
  quantity: number;
  productId: string;
  currency?: string;
  cart?: CartItem[];
}

interface ApplicationEventOptions {
  eventName: string;
  value: number;
  quantity: number;
  productId: string;
  currency?: string;
  cart?: CartItem[];
}

interface SubmissionEventOptions {
  eventName: string;
  value: number;
  cart: CartItem[];
  currency?: string;
}

interface UserEventOptions {
  eventName: string;
  cuidType: CuidType;
  cuid?: string;
  secondaryIdentifiers?: {
    type: string;
    value: string;
  }[];
}

interface RemoveFromCartEventOptions {
  eventName: string;
  value: number;
  quantity: number;
  productId: string;
  currency?: string;
  cart?: CartItem[];
}

interface SyncCartEventOptions {
  eventName: string;
  value: number;
  cart: CartItem[];
  currency?: string;
}

interface InformAffinityEventOptions {
  eventName: string;
  data: {
    attribute: string;
    values: string[];
  }[];
  source?: string;
}

interface AddToWishListEventOptions {
  eventName: string;
  productId: string;
  size?: string;
}

interface VideoWatchEventOptions {
  eventName: string;
  itemId: string;
  autoplay: boolean;
  progress: VideoProgressType;
  progressPercent: number;
  categories?: string[];
}

interface KeywordSearchEventOptions {
  eventName: string;
  keywords: string;
}

interface ChangeAttributesEventOptions {
  eventName: string;
  attributeType: string;
  attributeValue: string;
}

interface FilterItemsEventOptions {
  eventName: string;
  filterType: string;
  filterStringValue?: string;
  filterNumericValue?: number;
}

interface SortItemsEventOptions {
  eventName: string;
  sortBy: string;
  sortOrder: SortOrderType;
}

interface PromoCodeEnterEventOptions {
  eventName: string;
  code: string;
}

interface CustomEventOptions {
  eventName: string;
  properties: Record<string, any>;
}

// Initialize the SDK
export async function initialize(
  options: Omit<InitializeOptions, "apiKey">
): Promise<void> {
  try {
    await DynamicYieldModule.initialize({
      ...options,
      apiKey,
    });
    console.log("[DY] SDK initialized");
  } catch (error) {
    console.error("[DY] Error initializing SDK:", error);
  }
}

// Choose variations
export async function chooseVariations(
  options: ChooseVariationsOptions
): Promise<any> {
  try {
    await DynamicYieldModule.chooseVariations(options);
    console.log("[DY] Variations chosen");
  } catch (error) {
    console.error("[DY] Error choosing variations:", error);
  }
}

// Report pageviews
export async function reportHomePageView(location: string): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportHomePageView({ location });
    if (res.status === "success") {
      console.log("[DY] Home page view reported");
    } else if (res.status === "warning") {
      console.log("[DY] Home page view reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error(error);
  }
}

export async function reportCartPageView(
  location: string,
  cart: string[]
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportCartPageView({ location, cart });
    if (res.status === "success") {
      console.log("[DY] Cart page view reported");
    } else if (res.status === "warning") {
      console.log("[DY] Cart page view reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting cart page view:", error);
  }
}

export async function reportOtherPageView(
  location: string,
  data: string
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportOtherPageView({
      location,
      data,
    });
    if (res.status === "success") {
      console.log("[DY] Other page view reported");
    } else if (res.status === "warning") {
      console.log("[DY] Other page view reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting other page view:", error);
  }
}

export async function reportProductPageview(
  location: string,
  sku: string
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportProductPageview({
      location,
      sku,
    });
    if (res.status === "success") {
      console.log("[DY] Product page view reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Product page view reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting product page view:", error);
  }
}

export async function reportCategoryPageView(
  location: string,
  categories: string[]
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportCategoryPageView({
      location,
      categories,
    });
    if (res.status === "success") {
      console.log("[DY] Category page view reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Category page view reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting category page view:", error);
  }
}

// Report engagements
export async function reportImpression(
  decisionId: string,
  variations?: number[],
  branchId?: string,
  dayPart?: DayPart
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportImpression({
      decisionId,
      variations,
      branchId,
      dayPart,
    });
    if (res.status === "success") {
      console.log("[DY] Impression reported");
    } else if (res.status === "warning") {
      console.log("[DY] Impression reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting impression:", error);
  }
}

export async function reportClick(
  decisionId: string,
  variation?: number,
  branchId?: string,
  dayPart?: DayPart
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportClick({
      decisionId,
      variation,
      branchId,
      dayPart,
    });
    if (res.status === "success") {
      console.log("[DY] Click reported");
    } else if (res.status === "warning") {
      console.log("[DY] Click reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting click:", error);
  }
}

export async function reportSlotClick(
  slotId: string,
  variation?: number,
  branchId?: string,
  dayPart?: DayPart
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportSlotClick({
      variation,
      slotId,
      branchId,
      dayPart,
    });
    if (res.status === "success") {
      console.log("[DY] Slot click reported");
    } else if (res.status === "warning") {
      console.log("[DY] Slot click reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting slot click:", error);
  }
}

export async function reportSlotImpression(
  slotsIds: string[],
  variation?: number,
  branchId?: string,
  dayPart?: DayPart
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportSlotImpression({
      variation,
      slotsIds,
      branchId,
      dayPart,
    });
    if (res.status === "success") {
      console.log("[DY] Slot impression reported");
    } else if (res.status === "warning") {
      console.log("[DY] Slot impression reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting slot impression:", error);
  }
}

// Report events
export async function reportPurchaseEvent(
  options: PurchaseEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportPurchaseEvent(options);
    if (res.status === "success") {
      console.log("[DY] Purchase event reported");
    } else if (res.status === "warning") {
      console.log("[DY] Purchase event reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting purchase event:", error);
  }
}

export async function reportAddToCartEvent(
  options: AddToCartEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportAddToCartEvent(options);
    if (res.status === "success") {
      console.log("[DY] Add to cart event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Add to cart event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting add to cart event:", error);
  }
}

export async function reportApplicationEvent(
  options: ApplicationEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportApplicationEvent(options);
    if (res.status === "success") {
      console.log("[DY] Application event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Application event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting application event:", error);
  }
}

export async function reportSubmissionEvent(
  options: SubmissionEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportSubmissionEvent(options);
    if (res.status === "success") {
      console.log("[DY] Submission event reported");
    } else if (res.status === "warning") {
      console.log("[DY] Submission event reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting submission event:", error);
  }
}

export async function reportSignUpEvent(
  options: UserEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportSignUpEvent(options);
    if (res.status === "success") {
      console.log("[DY] Sign up event reported");
    } else if (res.status === "warning") {
      console.log("[DY] Sign up event reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting sign up event:", error);
  }
}

export async function reportLoginEvent(
  options: UserEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportLoginEvent(options);
    if (res.status === "success") {
      console.log("[DY] Login event reported");
    } else if (res.status === "warning") {
      console.log("[DY] Login event reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting login event:", error);
  }
}

export async function reportIdentifyUserEvent(
  options: UserEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportIdentifyUserEvent(options);
    if (res.status === "success") {
      console.log("[DY] Identify user event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Identify user event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting identify user event:", error);
  }
}

export async function reportNewsletterEvent(
  options: UserEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportNewsletterEvent(options);
    if (res.status === "success") {
      console.log("[DY] Newsletter event reported");
    } else if (res.status === "warning") {
      console.log("[DY] Newsletter event reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting newsletter event:", error);
  }
}

export async function reportRemoveFromCartEvent(
  options: RemoveFromCartEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportRemoveFromCartEvent(options);
    if (res.status === "success") {
      console.log("[DY] Remove from cart event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Remove from cart event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting remove from cart event:", error);
  }
}

export async function reportSyncCartEvent(
  options: SyncCartEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportSyncCartEvent(options);
    if (res.status === "success") {
      console.log("[DY] Sync cart event reported");
    } else if (res.status === "warning") {
      console.log("[DY] Sync cart event reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting sync cart event:", error);
  }
}

export async function reportMessageOptInEvent(
  options: UserEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportMessageOptInEvent(options);
    if (res.status === "success") {
      console.log("[DY] Message opt-in event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Message opt-in event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting message opt-in event:", error);
  }
}

export async function reportMessageOptOutEvent(
  options: UserEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportMessageOptOutEvent(options);
    if (res.status === "success") {
      console.log("[DY] Message opt-out event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Message opt-out event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting message opt-out event:", error);
  }
}

export async function reportInformAffinityEvent(
  options: InformAffinityEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportInformAffinityEvent(options);
    if (res.status === "success") {
      console.log("[DY] Inform affinity event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Inform affinity event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting inform affinity event:", error);
  }
}

export async function reportAddToWishListEvent(
  options: AddToWishListEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportAddToWishListEvent(options);
    if (res.status === "success") {
      console.log("[DY] Add to wishlist event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Add to wishlist event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting add to wishlist event:", error);
  }
}

export async function reportVideoWatchEvent(
  options: VideoWatchEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportVideoWatchEvent(options);
    if (res.status === "success") {
      console.log("[DY] Video watch event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Video watch event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting video watch event:", error);
  }
}

export async function reportKeywordSearchEvent(
  options: KeywordSearchEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportKeywordSearchEvent(options);
    if (res.status === "success") {
      console.log("[DY] Keyword search event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Keyword search event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting keyword search event:", error);
  }
}

export async function reportChangeAttributesEvent(
  options: ChangeAttributesEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportChangeAttributesEvent(options);
    if (res.status === "success") {
      console.log("[DY] Change attributes event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Change attributes event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting change attributes event:", error);
  }
}

export async function reportFilterItemsEvent(
  options: FilterItemsEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportFilterItemsEvent(options);
    if (res.status === "success") {
      console.log("[DY] Filter items event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Filter items event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting filter items event:", error);
  }
}

export async function reportSortItemsEvent(
  options: SortItemsEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportSortItemsEvent(options);
    if (res.status === "success") {
      console.log("[DY] Sort items event reported");
    } else if (res.status === "warning") {
      console.log("[DY] Sort items event reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting sort items event:", error);
  }
}

export async function reportPromoCodeEnterEvent(
  options: PromoCodeEnterEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportPromoCodeEnterEvent(options);
    if (res.status === "success") {
      console.log("[DY] Promo code enter event reported");
    } else if (res.status === "warning") {
      console.log(
        "[DY] Promo code enter event reported with warnings",
        res.warnings
      );
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting promo code enter event:", error);
  }
}

export async function reportCustomEvent(
  options: CustomEventOptions
): Promise<void> {
  try {
    const res = await DynamicYieldModule.reportCustomEvent(options);
    if (res.status === "success") {
      console.log("[DY] Custom event reported");
    } else if (res.status === "warning") {
      console.log("[DY] Custom event reported with warnings", res.warnings);
    } else {
      throw new Error(res?.error);
    }
  } catch (error) {
    console.error("[DY] Error reporting custom event:", error);
  }
}

// Set log level
export function setLogLevel(logLevel: LogLevel): void {
  try {
    DynamicYieldModule.setLogLevel({ logLevel });
    console.log("[DY] Log level set");
  } catch (error) {
    console.error("[DY] Error setting log level:", error);
  }
}

// Reset User ID and Session ID
export function resetUserIdAndSessionId(): void {
  try {
    DynamicYieldModule.resetUserIdAndSessionId();
    console.log("[DY] User ID and session ID reset");
  } catch (error) {
    console.error("[DY] Error resetting user ID and session ID:", error);
  }
}

// Reset DY ID and Session ID
export function resetDyidAndSessionId(): void {
  try {
    DynamicYieldModule.resetDyidAndSessionId();
    console.log("[DY] DY ID and session ID reset");
  } catch (error) {
    console.error("[DY] Error resetting DY ID and session ID:", error);
  }
}

// Hook to initialize the SDK with environment variables
export function useInitialize() {
  useFocusEffect(
    useCallback(() => {
      initialize({
        dataCenter: "EU",
        deviceType: "SMARTPHONE",
        channel: "APP",
        locale: "en-GB",
      });
    }, [])
  );
}

export function useHomeScreenView(location: string): void {
  useFocusEffect(
    useCallback(() => {
      reportHomePageView(location);
    }, [location])
  );
}
