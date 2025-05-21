import { ThemedText } from "@/components/ThemedText";
import { ThemedView } from "@/components/ThemedView";
import {
  initialize,
  reportAddToCartEvent,
  reportCustomEvent,
  reportHomePageView,
  reportPurchaseEvent,
  resetUserIdAndSessionId,
  setLogLevel,
} from "@/modules/dynamic-yield";
import {
  Platform,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
} from "react-native";

export default function HomeScreen() {
  // Initialize Dynamic Yield SDK
  // useInitialize();

  const handleInitialize = async () => {
    initialize({
      dataCenter: "US",
      deviceType: "SMARTPHONE",
      channel: "APP",
      locale: "en-GB",
    })
      .then((res) => {
        console.log(`[DY] SDK initialized`, res);
      })
      .catch((err) => {
        console.log(`[DY] SDK initialization failed: ${err}`);
      });
  };

  const handleTestAddToCart = () => {
    reportAddToCartEvent({
      eventName: "test_add_to_cart",
      value: 99,
      quantity: 1,
      productId: "test_product_123",
      currency: "USD",
    });
  };

  const handleTestPurchase = () => {
    reportPurchaseEvent({
      eventName: "test_purchase",
      value: 99,
      cart: [
        {
          productId: "test_product_123",
          quantity: 1,
          itemPrice: 99,
        },
      ],
      currency: "USD",
    });
  };

  const handleTestCustomEvent = () => {
    reportCustomEvent({
      eventName: "test_custom_event",
      properties: {
        testProperty: "testValue",
        timestamp: new Date().toISOString(),
      },
    });
  };

  const handleSetLogLevel = (
    level: "VERBOSE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "NONE"
  ) => {
    setLogLevel(level);
  };

  return (
    <ScrollView style={styles.container}>
      <ThemedView style={styles.header}>
        <ThemedText type="title">Dynamic Yield Debug Menu</ThemedText>
      </ThemedView>

      <ThemedView style={styles.section}>
        <ThemedText type="subtitle">SDK Status</ThemedText>
        <ThemedText>Platform: {Platform.OS}</ThemedText>
        <ThemedText>
          API Key: {Platform.OS === "ios" ? "iOS Key" : "Android Key"}
        </ThemedText>
      </ThemedView>

      <ThemedView style={styles.section}>
        <ThemedText type="subtitle">Initialize SDK</ThemedText>
        <TouchableOpacity style={styles.button} onPress={handleInitialize}>
          <ThemedText>Initialize SDK</ThemedText>
        </TouchableOpacity>
      </ThemedView>

      <ThemedView style={styles.section}>
        <ThemedText type="subtitle">Reset User ID and Session ID</ThemedText>
        <TouchableOpacity
          style={styles.button}
          onPress={resetUserIdAndSessionId}
        >
          <ThemedText>Reset User ID and Session ID</ThemedText>
        </TouchableOpacity>
      </ThemedView>

      <ThemedView style={styles.section}>
        <ThemedText type="subtitle">Log Level</ThemedText>
        <ThemedView style={styles.buttonRow}>
          <TouchableOpacity
            style={styles.button}
            onPress={() => handleSetLogLevel("VERBOSE")}
          >
            <ThemedText>VERBOSE</ThemedText>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.button}
            onPress={() => handleSetLogLevel("DEBUG")}
          >
            <ThemedText>DEBUG</ThemedText>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.button}
            onPress={() => handleSetLogLevel("INFO")}
          >
            <ThemedText>INFO</ThemedText>
          </TouchableOpacity>
        </ThemedView>
      </ThemedView>

      <ThemedView style={styles.section}>
        <ThemedText type="subtitle">Test Events</ThemedText>
        <TouchableOpacity style={styles.button} onPress={handleTestAddToCart}>
          <ThemedText>Test Add to Cart</ThemedText>
        </TouchableOpacity>
        <TouchableOpacity style={styles.button} onPress={handleTestPurchase}>
          <ThemedText>Test Purchase</ThemedText>
        </TouchableOpacity>
        <TouchableOpacity style={styles.button} onPress={handleTestCustomEvent}>
          <ThemedText>Test Custom Event</ThemedText>
        </TouchableOpacity>
      </ThemedView>

      <ThemedView style={styles.section}>
        <ThemedText type="subtitle">Page Views</ThemedText>
        <TouchableOpacity
          style={styles.button}
          onPress={() => reportHomePageView("home")}
        >
          <ThemedText>Report Home Page View</ThemedText>
        </TouchableOpacity>
      </ThemedView>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    padding: 16,
    alignItems: "center",
  },
  section: {
    padding: 16,
    gap: 8,
  },
  buttonRow: {
    flexDirection: "row",
    gap: 8,
    flexWrap: "wrap",
  },
  button: {
    backgroundColor: "#A1CEDC",
    padding: 12,
    borderRadius: 8,
    alignItems: "center",
    marginVertical: 4,
  },
});
