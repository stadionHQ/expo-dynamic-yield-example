import ExpoModulesCore
import DyLibrary

public class DynamicYieldModule: Module {
  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.

  public func definition() -> ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('DynamicYield')` in JavaScript.
    Name("DynamicYield")

    // Initialize the SDK
    AsyncFunction("initialize") { (args: [Any]) in
      guard let apiKey = args[0] as? String,
            let dataCenter = args[1] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let deviceType = args[2] as? String
      let channel = args[3] as? String
      let ip = args[4] as? String
      let locale = args[5] as? String
      let isImplicitPageview = args[6] as? Bool
      let isImplicitImpressionMode = args[7] as? Bool
      let customUrl = args[8] as? String
      let sharedDevice = args[9] as? Bool
      let deviceId = args[10] as? String
      
      // Convert dataCenter string to enum
      let dataCenterEnum: DataCenter = dataCenter.lowercased() == "eu" ? .eu : .us
      
      // Convert deviceType string to enum if provided
      let deviceTypeEnum: DeviceType? = {
        guard let type = deviceType?.lowercased() else { return nil }
        switch type {
        case "smartphone": return .smartphone
        case "tablet": return .tablet
        case "kiosk": return .kiosk
        case "odmb": return .odmb
        default: return nil
        }
      }()
      
      // Convert channel string to enum if provided
      let channelEnum: Channel? = {
        guard let ch = channel?.lowercased() else { return nil }
        switch ch {
        case "app": return .app
        case "kiosk": return .kiosk
        case "drive_thru": return .driveThru
        default: return nil
        }
      }()
      
      DYSdk.initialize(
        apiKey: apiKey,
        dataCenter: dataCenterEnum,
        deviceType: deviceTypeEnum,
				sharedDevice: sharedDevice,
				deviceId: deviceId,
				channel: channelEnum ?? Channel.app,
				ip: ip,
				locale: locale,
				isImplicitPageview: isImplicitPageview,
				isImplicitImpressionMode: isImplicitImpressionMode,
				customUrl: customUrl
      )
    }

    // Choose Variations
    AsyncFunction("chooseVariations") { (args: [Any]) async throws -> [String: Any] in
      guard let pageLocation = args[0] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid pageLocation argument"])
      }
      
      let pageReferrer = args[1] as? String
      let selectorNames = args[2] as? [String]
      let selectorGroups = args[3] as? [String]
      let selectorPreviews = args[4] as? [String]
      let dayPart = args[5] as? String
      let cart = args[6] as? [[String: Any]]
      let branchId = args[7] as? String
      let options = args[8] as? [String: Any]
      let pageAttributes = args[9] as? [String: Any]
      let recsProductData = args[10] as? [String: Any]
      
      // Convert cart items
      let cartItems = cart?.map { item in
        CartItem(
          productId: item["productId"] as? String ?? "",
          quantity: UInt(truncating: item["quantity"] as? NSNumber ?? 0),
          itemPrice: Float(truncating: item["itemPrice"] as? NSNumber ?? 0),
          innerProducts: (item["innerProducts"] as? [[String: Any]])?.map { inner in
            CartInnerItem(
              productId: inner["productId"] as? String ?? "",
              quantity: UInt(truncating: inner["quantity"] as? NSNumber ?? 0),
              itemPrice: Float(truncating: inner["itemPrice"] as? NSNumber ?? 0)
            )
          }
        )
      }
      
      // Convert day part
      let dayPartEnum: DayPart? = {
        guard let part = dayPart?.lowercased() else { return nil }
        switch part {
        case "breakfast": return .breakfast
        case "lunch": return .lunch
        case "dinner": return .dinner
        default: return nil
        }
      }()
      
      // Convert choose options
			var chooseOptions = ChooseOptions()
      if let opts = options {
        chooseOptions.isImplicitPageview = opts["isImplicitPageview"] as? Bool ?? false
        chooseOptions.returnAnalyticsMetadata = opts["returnAnalyticsMetadata"] as? Bool ?? false
        chooseOptions.isImplicitImpressionMode = opts["isImplicitImpressionMode"] as? Bool ?? false
      }
      
      // Convert recs product data
      let recsProductDataOptions: RecsProductDataOptions? = {
        guard let data = recsProductData else { return nil }
        return RecsProductDataOptions(
          skusOnly: data["skusOnly"] as? Bool,
          fieldFilter: data["fieldFilter"] as? [String]
        )
      }()
      
      // Convert page attributes
      let pageAttributesMap = pageAttributes?.mapValues { value in
        if let stringValue = value as? String {
          return PageAttribute(stringValue)
        } else if let numberValue = value as? Int {
          return PageAttribute(numberValue)
        }
        return PageAttribute("")
      }
      
      let result = await DYSdk.shared().choose.chooseVariations(
        selectorNames: selectorNames,
        page: Page.homePage(pageLocation: pageLocation),
        selectorGroups: selectorGroups,
        selectorPreviews: selectorPreviews,
        dayPart: dayPartEnum,
        cart: cartItems,
        branchId: branchId,
        options: chooseOptions,
        pageAttributes: pageAttributesMap,
        recsProductData: recsProductDataOptions
      )
      
      // Convert result to dictionary
      var resultDict: [String: Any] = [
        "status": result.status.rawValue
      ]
      
      if let choices = result.choices {
        resultDict["choices"] = choices.map { choice in
          [
            "id": choice.id,
            "decisionId": choice.decisionId,
            "name": choice.name,
            "type": choice.type.rawValue,
            "variations": choice.variations.map { variation in
              var variationDict: [String: Any] = [
                "id": variation.id
              ]
              
              if let analyticsMetadata = variation.analyticsMetadata {
                variationDict["analyticsMetadata"] = analyticsMetadata
              }
              
              let payload = variation.payload
              switch payload {
              case let customPayload as CustomJsonPayload:
                variationDict["payload"] = [
                  "type": "DECISION",
                  "data": customPayload.data
                ]
              case let recsPayload as RecsPayload:
                variationDict["payload"] = [
                  "type": "RECS_DECISION",
                  "data": [
                    "custom": recsPayload.data.custom,
                    "slots": recsPayload.data.slots
                  ]
                ]
              case let storeRecsPayload as StoreRecsPayload:
                variationDict["payload"] = [
                  "type": "STORE_RECS_DECISION",
                  "data": [
                    "custom": storeRecsPayload.data.custom,
                    "slots": storeRecsPayload.data.slots
                  ]
                ]
              default:
                break
              }
              
              return variationDict
            },
            "groups": choice.groups
          ]
        }
      }
      
      if let warnings = result.warnings {
        resultDict["warnings"] = warnings.map { warning in
          [
            "warning": String(describing: warning)
          ]
        }
      }
      
      if let error = result.error {
        resultDict["error"] = error.localizedDescription
      }
      
      if let rawNetworkData = result.rawNetworkData {
        resultDict["rawNetworkData"] = [
					"code": rawNetworkData.code,
					"body": rawNetworkData.body
        ]
      }
      
      return resultDict
    }

    // Report Pageview
    AsyncFunction("reportHomePageView") { (
      location: String
    ) async throws in
			await DYSdk.shared().pageViews.reportHomePageView(pageLocation: location)
    }
		
		// Report Pageview
		AsyncFunction("reportCartPageView") { (
			location: String,
			cart: [String]
		) async throws in
			await DYSdk.shared().pageViews.reportCartPageView(pageLocation: location, cart: cart)
		}
		
		// Report Pageview
		AsyncFunction("reportOtherPageView") { (
			location: String,
			data: String
		) async throws in
			await DYSdk.shared().pageViews.reportOtherPageView(pageLocation: location, data: data)
		}
		
		// Report Pageview
		AsyncFunction("reportProductPageView") { (
			location: String,
			sku: String
		) async throws in
			await DYSdk.shared().pageViews.reportProductPageView(pageLocation: location, sku: sku)
		}

		// Report Pageview
		AsyncFunction("reportCategoryPageView") { (
			location: String,
			categories: [String]
		) async throws in
			await DYSdk.shared().pageViews.reportCategoryPageView(pageLocation: location, categories: categories)
		}

		// Report Impression Engagement
		AsyncFunction("reportImpression") { (args: [Any]) async throws in
			guard let decisionId = args[0] as? String else {
				throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid decisionId argument"])
			}
			
			let variations = args[1] as? [Int]
			let branchId = args[2] as? String
			let dayPart = args[3] as? String
			
			let dayPartEnum: DayPart? = {
				guard let part = dayPart?.lowercased() else { return nil }
				switch part {
				case "breakfast": return .breakfast
				case "lunch": return .lunch
				case "dinner": return .dinner
				default: return nil
				}
			}()
			
			await DYSdk.shared().engagements.reportImpression(
				decisionId: decisionId,
				variations: variations,
				branchId: branchId,
				dayPart: dayPartEnum
			)
		}

    // Report Click Engagement
		AsyncFunction("reportClick") { (args: [Any]) async throws in
			guard let decisionId = args[0] as? String else {
				throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid decisionId argument"])
			}
			let variation = args[1] as? Int
			let branchId = args[2] as? String
			let dayPart = args[3] as? String
			
			let dayPartEnum: DayPart? = {
				guard let part = dayPart?.lowercased() else { return nil }
				switch part {
				case "breakfast": return .breakfast
				case "lunch": return .lunch
				case "dinner": return .dinner
				default: return nil
				}
			}()

			await DYSdk.shared().engagements.reportClick(
				decisionId: decisionId,
				variation: variation,
				branchId: branchId,
				dayPart: dayPartEnum
			)
		}

    // Report Slot Click Engagement
		AsyncFunction("reportSlotClick") {(args: [Any]) async throws in
      let variation = args[0] as? Int
			guard let slotId = args[1] as? String else {
				throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid slotId argument"])
			}
      let branchId = args[2] as? String
      let dayPart = args[3] as? String
			
			let dayPartEnum: DayPart? = {
				guard let part = dayPart?.lowercased() else { return nil }
				switch part {
				case "breakfast": return .breakfast
				case "lunch": return .lunch
				case "dinner": return .dinner
				default: return nil
				}
			}()

			await DYSdk.shared().engagements.reportSlotClick(
				variation: variation,
				slotId: slotId,
				branchId: branchId,
				dayPart: dayPartEnum
			)
		}

		// Report Slot Impression Engagement
		AsyncFunction("reportSlotImpression") { (args: [Any]) async throws in
			let variation = args[0] as? Int
			guard let slotsIds = args[1] as? [String] else {
				throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid slotsIds argument"])
			}
			let branchId = args[2] as? String
			let dayPart = args[3] as? String
			
			let dayPartEnum: DayPart? = {
				guard let part = dayPart?.lowercased() else { return nil }
				switch part {
				case "breakfast": return .breakfast
				case "lunch": return .lunch
				case "dinner": return .dinner
				default: return nil
				}
			}()

			await DYSdk.shared().engagements.reportSlotImpression(
				variation: variation,
				slotsIds: slotsIds,
				branchId: branchId,
				dayPart: dayPartEnum
			)
		}

    // Set Log Level
    Function("setLogLevel") { (logLevel: String) in
      let logLevelEnum: LogLevel = {
        switch logLevel.lowercased() {
        case "debug": return .debug
        case "info": return .info
        case "warn": return .warning
        case "error": return .error
				case "none": return .off
        default: return .info
        }
      }()
      
			DYSdk.setLogLevel(level: logLevelEnum)
    }

    // Reset User ID and Session ID
    Function("resetUserIdAndSessionId") { () in
      DYSdk.shared().resetUserIdAndSessionId()
    }

    // Report Purchase Event
    AsyncFunction("reportPurchaseEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let value = args[1] as? Float,
            let cart = args[2] as? [[String: Any]] else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let currency = args[3] as? String
      let uniqueTransactionId = args[4] as? String
      
      let cartItems = cart.map { item in
        CartInnerItem(
          productId: item["productId"] as? String ?? "",
          quantity: UInt(truncating: item["quantity"] as? NSNumber ?? 0),
          itemPrice: Float(truncating: item["itemPrice"] as? NSNumber ?? 0)
        )
      }
      
      let currencyEnum: CurrencyType? = {
        guard let curr = currency?.lowercased() else { return nil }
        return CurrencyType(rawValue: curr)
      }()
      
      await DYSdk.shared().events.reportPurchaseEvent(
        eventName: eventName,
        value: value,
        currency: currencyEnum,
        uniqueTransactionId: uniqueTransactionId,
        cart: cartItems
      )
    }

    // Report Add to Cart Event
    AsyncFunction("reportAddToCartEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let value = args[1] as? Float,
            let quantity = args[2] as? Int,
            let productId = args[3] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let currency = args[4] as? String
      let cart = args[5] as? [[String: Any]]
      
      let cartItems = cart?.map { item in
        CartInnerItem(
          productId: item["productId"] as? String ?? "",
          quantity: UInt(truncating: item["quantity"] as? NSNumber ?? 0),
          itemPrice: Float(truncating: item["itemPrice"] as? NSNumber ?? 0)
        )
      }
      
      let currencyEnum: CurrencyType? = {
        guard let curr = currency?.lowercased() else { return nil }
        return CurrencyType(rawValue: curr)
      }()
      
      await DYSdk.shared().events.reportAddToCartEvent(
        eventName: eventName,
        value: value,
        currency: currencyEnum,
        productId: productId,
        quantity: UInt(quantity),
        cart: cartItems
      )
    }

    // Report Application Event
    AsyncFunction("reportApplicationEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let value = args[1] as? Float,
            let quantity = args[2] as? Int,
            let productId = args[3] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let currency = args[4] as? String
      let cart = args[5] as? [[String: Any]]
      
      let cartItems = cart?.map { item in
        CartInnerItem(
          productId: item["productId"] as? String ?? "",
          quantity: UInt(truncating: item["quantity"] as? NSNumber ?? 0),
          itemPrice: Float(truncating: item["itemPrice"] as? NSNumber ?? 0)
        )
      }
      
      let currencyEnum: CurrencyType? = {
        guard let curr = currency?.lowercased() else { return nil }
        return CurrencyType(rawValue: curr)
      }()
      
      await DYSdk.shared().events.reportApplicationEvent(
        eventName: eventName,
        value: value,
        currency: currencyEnum,
        productId: productId,
        quantity: UInt(quantity),
        cart: cartItems
      )
    }

    // Report Submission Event
    AsyncFunction("reportSubmissionEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let value = args[1] as? Float,
            let cart = args[2] as? [[String: Any]] else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let currency = args[3] as? String
      
      let cartItems = cart.map { item in
        CartInnerItem(
          productId: item["productId"] as? String ?? "",
          quantity: UInt(truncating: item["quantity"] as? NSNumber ?? 0),
          itemPrice: Float(truncating: item["itemPrice"] as? NSNumber ?? 0)
        )
      }
      
      let currencyEnum: CurrencyType? = {
        guard let curr = currency?.lowercased() else { return nil }
        return CurrencyType(rawValue: curr)
      }()
      
      await DYSdk.shared().events.reportSubmissionEvent(
        eventName: eventName,
        value: value,
        currency: currencyEnum,
        cart: cartItems
      )
    }

    // Report Sign Up Event
    AsyncFunction("reportSignUpEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let cuidType = args[1] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      await DYSdk.shared().events.reportSignUpEvent(
        eventName: eventName,
        cuidType: cuidType
      )
    }

    // Report Login Event
    AsyncFunction("reportLoginEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let cuidType = args[1] as? String
      let cuid = args[2] as? String
      let secondaryIdentifiers = args[3] as? [[String: Any]]
      
      let secondaryIds = secondaryIdentifiers?.map { identifier in
        SecondaryIdentifier(
          type: identifier["type"] as? String ?? "",
          value: identifier["value"] as? String ?? ""
        )
      }
      
      await DYSdk.shared().events.reportLoginEvent(
        eventName: eventName,
        cuidType: cuidType,
        cuid: cuid,
        secondaryIdentifiers: secondaryIds
      )
    }

    // Report Identify User Event
    AsyncFunction("reportIdentifyUserEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let cuidType = args[1] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let cuid = args[2] as? String
      let secondaryIdentifiers = args[3] as? [[String: Any]]
      
      let secondaryIds = secondaryIdentifiers?.map { identifier in
        SecondaryIdentifier(
          type: identifier["type"] as? String ?? "",
          value: identifier["value"] as? String ?? ""
        )
      }
      
      await DYSdk.shared().events.reportIdentifyUserEvent(
        eventName: eventName,
        cuidType: cuidType,
        cuid: cuid,
        secondaryIdentifiers: secondaryIds
      )
    }

    // Report Newsletter Event
    AsyncFunction("reportNewsletterEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let cuidType = args[1] as? String
      let cuid = args[2] as? String
      let secondaryIdentifiers = args[3] as? [[String: Any]]
      
      let secondaryIds = secondaryIdentifiers?.map { identifier in
        SecondaryIdentifier(
          type: identifier["type"] as? String ?? "",
          value: identifier["value"] as? String ?? ""
        )
      }
      
      await DYSdk.shared().events.reportNewsletterEvent(
        eventName: eventName,
        cuidType: cuidType,
        cuid: cuid,
        secondaryIdentifiers: secondaryIds
      )
    }

    // Report Remove from Cart Event
    AsyncFunction("reportRemoveFromCartEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let value = args[1] as? Float,
            let quantity = args[2] as? Int,
            let productId = args[3] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let currency = args[4] as? String
      let cart = args[5] as? [[String: Any]]
      
      let cartItems = cart?.map { item in
        CartInnerItem(
          productId: item["productId"] as? String ?? "",
          quantity: UInt(truncating: item["quantity"] as? NSNumber ?? 0),
          itemPrice: Float(truncating: item["itemPrice"] as? NSNumber ?? 0)
        )
      }
      
      let currencyEnum: CurrencyType? = {
        guard let curr = currency?.lowercased() else { return nil }
        return CurrencyType(rawValue: curr)
      }()
      
      await DYSdk.shared().events.reportRemoveFromCartEvent(
        eventName: eventName,
        value: value,
        currency: currencyEnum,
        productId: productId,
        quantity: UInt(quantity),
        cart: cartItems
      )
    }

    // Report Sync Cart Event
    AsyncFunction("reportSyncCartEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let value = args[1] as? Float,
            let cart = args[2] as? [[String: Any]] else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let currency = args[3] as? String
      
      let cartItems = cart.map { item in
        CartInnerItem(
          productId: item["productId"] as? String ?? "",
          quantity: UInt(truncating: item["quantity"] as? NSNumber ?? 0),
          itemPrice: Float(truncating: item["itemPrice"] as? NSNumber ?? 0)
        )
      }
      
      let currencyEnum: CurrencyType? = {
        guard let curr = currency?.lowercased() else { return nil }
        return CurrencyType(rawValue: curr)
      }()
      
      await DYSdk.shared().events.reportSyncCartEvent(
        eventName: eventName,
        value: value,
        currency: currencyEnum,
        cart: cartItems
      )
    }

    // Report Message Opt In Event
    AsyncFunction("reportMessageOptInEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let cuidType = args[1] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let plainTextEmail = args[2] as? String
      let externalId = args[3] as? String
      
      let cuidTypeEnum: CuidType = {
        switch cuidType.lowercased() {
        case "email": return .email
        case "external": return .external
        default: return .email
        }
      }()
      
      await DYSdk.shared().events.reportMessageOptInEvent(
        eventName: eventName,
        cuidType: cuidTypeEnum,
        plainTextEmail: plainTextEmail,
        externalId: externalId
      )
    }

    // Report Message Opt Out Event
    AsyncFunction("reportMessageOptOutEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let cuidType = args[1] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let plainTextEmail = args[2] as? String
      let externalId = args[3] as? String
      
      let cuidTypeEnum: CuidType = {
        switch cuidType.lowercased() {
        case "email": return .email
        case "external": return .external
        default: return .email
        }
      }()
      
      await DYSdk.shared().events.reportMessageOptOutEvent(
        eventName: eventName,
        cuidType: cuidTypeEnum,
        plainTextEmail: plainTextEmail,
        externalId: externalId
      )
    }

    // Report Inform Affinity Event
    AsyncFunction("reportInformAffinityEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let data = args[1] as? [[String: Any]] else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let source = args[2] as? String
      
      let affinityData = data.map { item in
        InformAffinityData(
          attribute: item["attribute"] as? String ?? "",
          values: item["values"] as? [String] ?? []
        )
      }
      
      await DYSdk.shared().events.reportInformAffinityEvent(
        eventName: eventName,
        source: source,
        data: affinityData
      )
    }

    // Report Add to Wish List Event
    AsyncFunction("reportAddToWishListEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let productId = args[1] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let size = args[2] as? String
      
      await DYSdk.shared().events.reportAddToWishListEvent(
        eventName: eventName,
        productId: productId,
        size: size
      )
    }

    // Report Video Watch Event
    AsyncFunction("reportVideoWatchEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let itemId = args[1] as? String,
            let autoplay = args[2] as? Bool,
            let progress = args[3] as? String,
            let progressPercent = args[4] as? Int else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let categories = args[5] as? [String]
      
      let progressEnum: VideoProgressType = {
        switch progress.lowercased() {
				case "start": return .videoStarted
				case "progress": return .videoProgress
				case "complete": return .videoFinished
        default: return .videoProgress
        }
      }()
      
      await DYSdk.shared().events.reportVideoWatchEvent(
        eventName: eventName,
        itemId: itemId,
        categories: categories,
        autoplay: autoplay,
        progress: progressEnum,
        progressPercent: UInt(progressPercent)
      )
    }

    // Report Keyword Search Event
    AsyncFunction("reportKeywordSearchEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let keywords = args[1] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      await DYSdk.shared().events.reportKeywordSearchEvent(
        eventName: eventName,
        keywords: keywords
      )
    }

    // Report Change Attributes Event
    AsyncFunction("reportChangeAttributesEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let attributeType = args[1] as? String,
            let attributeValue = args[2] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      await DYSdk.shared().events.reportChangeAttributesEvent(
        eventName: eventName,
        attributeType: attributeType,
        attributeValue: attributeValue
      )
    }

    // Report Filter Items Event
    AsyncFunction("reportFilterItemsEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let filterType = args[1] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let filterNumericValue = args[2] as? Int
      let filterStringValue = args[3] as? String
      
      await DYSdk.shared().events.reportFilterItemsEvent(
        eventName: eventName,
        filterType: filterType,
        filterNumericValue: filterNumericValue,
        filterStringValue: filterStringValue
      )
    }

    // Report Sort Items Event
    AsyncFunction("reportSortItemsEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let sortBy = args[1] as? String,
            let sortOrder = args[2] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      let sortOrderEnum: SortOrderType = {
        switch sortOrder.lowercased() {
        case "asc": return .asc
        case "desc": return .desc
        default: return .asc
        }
      }()
      
      await DYSdk.shared().events.reportSortItemsEvent(
        eventName: eventName,
        sortBy: sortBy,
        sortOrder: sortOrderEnum
      )
    }

    // Report Promo Code Enter Event
    AsyncFunction("reportPromoCodeEnterEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let code = args[1] as? String else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      await DYSdk.shared().events.reportPromoCodeEnterEvent(
        eventName: eventName,
        code: code
      )
    }

    // Report Custom Event
    AsyncFunction("reportCustomEvent") { (args: [Any]) async throws in
      guard let eventName = args[0] as? String,
            let properties = args[1] as? [String: Any] else {
        throw NSError(domain: "DynamicYield", code: 1, userInfo: [NSLocalizedDescriptionKey: "Invalid arguments"])
      }
      
      struct CustomEventProperties: CustomProperties {
        let properties: [String: Any]
        
        func encode(to encoder: Encoder) throws {
          var container = encoder.singleValueContainer()
          let jsonData = try JSONSerialization.data(withJSONObject: properties)
          try container.encode(jsonData)
        }
      }
      
      await DYSdk.shared().events.reportCustomEvents(
        eventName: eventName,
        properties: CustomEventProperties(properties: properties)
      )
    }
  }
}
