package expo.modules.dynamicyield

import com.dynamicyield.sdk.wrapper.core.DYSdk
import com.dynamicyield.sdk.wrapper.core.enums.*
import com.dynamicyield.sdk.wrapper.core.managers.logging.loggingManager.LogLevel
import com.dynamicyield.sdk.wrapper.core.models.choose.payload.CartItem
import com.dynamicyield.sdk.wrapper.core.models.choose.payload.ChooseOptions
import com.dynamicyield.sdk.wrapper.core.models.choose.payload.RecsProductDataOptions
import com.dynamicyield.sdk.wrapper.core.models.choose.response.CustomJsonPayload
import com.dynamicyield.sdk.wrapper.core.models.choose.response.RecsPayload
import com.dynamicyield.sdk.wrapper.core.models.choose.response.StoreRecsPayload
import com.dynamicyield.sdk.wrapper.core.models.common.payload.*
import com.dynamicyield.sdk.wrapper.core.models.event.CartInnerItem
import com.dynamicyield.sdk.wrapper.core.models.event.CustomProperties
import com.dynamicyield.sdk.wrapper.core.models.event.InformAffinityData
import com.dynamicyield.sdk.wrapper.core.models.event.SecondaryIdentifier
import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class DynamicYieldModule : Module() {
  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  override fun definition() = ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a
    // string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for
    // clarity.
    // The module will be accessible from `requireNativeModule('DynamicYield')` in JavaScript.
    Name("DynamicYield")

    // Initialize the SDK with required parameters
    Function("initialize") { config: Map<String, Any> ->
      val dySdk = DYSdk.getInstance()

      // Extract required parameters
      val apiKey = config["apiKey"] as String
      val dataCenter = config["dataCenter"] as String

      // Extract optional parameters
      val deviceType = config["deviceType"] as? String
      val channel = config["channel"] as String
      val ip = config["ip"] as? String
      val locale = config["locale"] as? String
      val isImplicitPageview = config["isImplicitPageview"] as? Boolean
      val isImplicitImpressionMode = config["isImplicitImpressionMode"] as? Boolean
      val customUrl = config["customUrl"] as? String
      val sharedDevice = config["sharedDevice"] as? Boolean
      val deviceId = config["deviceId"] as? String

      // Convert string dataCenter to enum
      val dataCenterEnum =
              when (dataCenter.uppercase()) {
                "US" -> DataCenter.US
                "EU" -> DataCenter.EU
                else -> throw IllegalArgumentException("Invalid data center. Must be 'US' or 'EU'")
              }

      // Convert string deviceType to enum if provided
      val deviceTypeEnum =
              deviceType?.let {
                when (it.uppercase()) {
                  "SMARTPHONE" -> DeviceType.SMARTPHONE
                  "TABLET" -> DeviceType.TABLET
                  "KIOSK" -> DeviceType.KIOSK
                  "ODMB" -> DeviceType.ODMB
                  else -> throw IllegalArgumentException("Invalid device type")
                }
              }

      // Convert string channel to enum if provided
      val channelEnum =
              channel.let {
                when (it.uppercase()) {
                  "APP" -> Channel.APP
                  "KIOSK" -> Channel.KIOSK
                  "DRIVE_THRU" -> Channel.DRIVE_THRU
                  else -> throw IllegalArgumentException("Invalid channel")
                }
              }

      // Initialize the SDK with required parameters
      val context =
              appContext.reactContext
                      ?: throw IllegalStateException("React context is not available")
      var res =
              dySdk.initialize(
                      apiKey = apiKey,
                      dataCenter = dataCenterEnum,
                      context = context,
                      deviceType = deviceTypeEnum,
                      channel = channelEnum,
                      ip = ip,
                      locale = locale,
                      isImplicitPageview = isImplicitPageview,
                      isImplicitImpressionMode = isImplicitImpressionMode,
                      customUrl = customUrl,
                      sharedDevice = sharedDevice,
                      deviceId = deviceId
              )
      if (!res) {
        throw Exception("Failed to initialize SDK")
      }
      mapOf("status" to "success")
    }

    // Choose Variations
    AsyncFunction("chooseVariations") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()

              // Extract required parameters
              val pageLocation = config["pageLocation"] as String

              // Extract optional parameters
              val pageReferrer = config["pageReferrer"] as? String
              val selectorNames = config["selectorNames"] as? List<String>
              val selectorGroups = config["selectorGroups"] as? List<String>
              val selectorPreviews = config["selectorPreviews"] as? List<String>
              val dayPart = config["dayPart"] as? String
              val cart = config["cart"] as? List<Map<String, Any>>
              val branchId = config["branchId"] as? String
              val options = config["options"] as? Map<String, Any>
              val pageAttributes = config["pageAttributes"] as? Map<String, Any>
              val recsProductData = config["recsProductData"] as? Map<String, Any>

              // Convert page attributes
              val pageAttributesMap =
                      pageAttributes?.mapValues { (_, value) ->
                        when (value) {
                          is String -> PageAttribute(value)
                          is Int -> PageAttribute(value)
                          else ->
                                  throw IllegalArgumentException(
                                          "Page attribute value must be string or number"
                                  )
                        }
                      }

              // Convert cart items
              val cartItems =
                      cart?.map { item ->
                        CartItem(
                                productId = item["productId"] as String,
                                quantity = (item["quantity"] as Double).toUInt(),
                                itemPrice = (item["itemPrice"] as Number).toFloat(),
                                innerProducts =
                                        (item["innerProducts"] as? List<Map<String, Any>>)?.map {
                                                inner ->
                                          CartInnerItem(
                                                  productId = inner["productId"] as String,
                                                  quantity = (inner["quantity"] as Double).toUInt(),
                                                  itemPrice =
                                                          (inner["itemPrice"] as Double).toFloat()
                                          )
                                        }
                        )
                      }

              // Convert day part
              val dayPartEnum =
                      dayPart?.let {
                        when (it.uppercase()) {
                          "BREAKFAST" -> DayPart.BREAKFAST
                          "LUNCH" -> DayPart.LUNCH
                          "DINNER" -> DayPart.DINNER
                          else -> throw IllegalArgumentException("Invalid day part")
                        }
                      }

              // Convert choose options
              val chooseOptions =
                      options?.let {
                        ChooseOptions(
                                isImplicitPageview = it["isImplicitPageview"] as? Boolean,
                                isImplicitImpressionMode =
                                        it["isImplicitImpressionMode"] as? Boolean,
                                returnAnalyticsMetadata = it["returnAnalyticsMetadata"] as? Boolean,
                        )
                      }

              // Convert recs product data
              val recsProductDataOptions =
                      recsProductData?.let {
                        RecsProductDataOptions(
                                fieldFilter = it["fieldFilter"] as? List<String>,
                                skusOnly = it["skusOnly"] as? Boolean
                        )
                      }

              val result =
                      dySdk.choose.chooseVariations(
                              page = Page.homePage(pageLocation, pageReferrer),
                              selectorNames = selectorNames,
                              selectorGroups = selectorGroups,
                              selectorPreviews = selectorPreviews,
                              dayPart = dayPartEnum,
                              cart = cartItems,
                              branchId = branchId,
                              options = chooseOptions,
                              pageAttributes = pageAttributesMap,
                              recsProductData = recsProductDataOptions
                      )

              // Convert result to a map that can be serialized to JSON
              mapOf(
                      "status" to result.status.name,
                      "choices" to
                              result.choices?.map { choice ->
                                mapOf(
                                        "id" to choice.id,
                                        "decisionId" to choice.decisionId,
                                        "name" to choice.name,
                                        "type" to choice.type.name,
                                        "variations" to
                                                choice.variations.map { variation ->
                                                  mapOf(
                                                          "id" to variation.id,
                                                          "analyticsMetadata" to
                                                                  variation.analyticsMetadata,
                                                          "payload" to
                                                                  when (val payload =
                                                                                  variation.payload
                                                                  ) {
                                                                    is CustomJsonPayload ->
                                                                            mapOf(
                                                                                    "type" to
                                                                                            "DECISION",
                                                                                    "data" to
                                                                                            payload.data
                                                                            )
                                                                    is RecsPayload ->
                                                                            mapOf(
                                                                                    "type" to
                                                                                            "RECS_DECISION",
                                                                                    "data" to
                                                                                            mapOf(
                                                                                                    "custom" to
                                                                                                            payload.data
                                                                                                                    .custom,
                                                                                                    "slots" to
                                                                                                            payload.data
                                                                                                                    .slots
                                                                                            )
                                                                            )
                                                                    is StoreRecsPayload ->
                                                                            mapOf(
                                                                                    "type" to
                                                                                            "STORE_RECS_DECISION",
                                                                                    "data" to
                                                                                            mapOf(
                                                                                                    "custom" to
                                                                                                            payload.data
                                                                                                                    .custom,
                                                                                                    "slots" to
                                                                                                            payload.data
                                                                                                                    .slots
                                                                                            )
                                                                            )
                                                                    else -> null
                                                                  }
                                                  )
                                                },
                                        "groups" to choice.groups
                                )
                              },
                      "warnings" to
                              result.warnings?.map { warning ->
                                mapOf("code" to warning.code, "message" to warning.message)
                              },
                      "error" to result.error?.message,
                      "rawNetworkData" to
                              result.rawNetworkData?.let { raw ->
                                mapOf("code" to raw.code, "body" to raw.body)
                              }
              )
            }

    // Report pageviews
    AsyncFunction("reportHomePageView") Coroutine
            { config: Map<String, Any> ->
              val location = config["location"] as String
              var result = DYSdk.getInstance().pageViews.reportHomePageView(location)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  // A pageview has been reported successfully
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  // A pageview has been reported successfully, however, warnings have been returned
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  // Failed
                  result.error?.let {
                    // Handle the error
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportCartPageView") Coroutine
            { config: Map<String, Any> ->
              val location = config["location"] as String
              val cart = config["cart"] as List<String>
              val result = DYSdk.getInstance().pageViews.reportCartPageView(location, cart)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportOtherPageView") Coroutine
            { config: Map<String, Any> ->
              val location = config["location"] as String
              val data = config["data"] as String
              val result = DYSdk.getInstance().pageViews.reportOtherPageView(location, data = data)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportProductPageView") Coroutine
            { config: Map<String, Any> ->
              val location = config["location"] as String
              val sku = config["sku"] as String
              val result = DYSdk.getInstance().pageViews.reportProductPageView(location, sku)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportCategoryPageView") Coroutine
            { config: Map<String, Any> ->
              val location = config["location"] as String
              val categories = config["categories"] as List<String>
              val result =
                      DYSdk.getInstance().pageViews.reportCategoryPageView(location, categories)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    // Report engagements
    AsyncFunction("reportImpression") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val decisionId = config["decisionId"] as String
              val variations = config["variations"] as? List<Int>
              val branchId = config["branchId"] as? String
              val dayPart = config["dayPart"] as? String

              val dayPartEnum =
                      dayPart?.let {
                        when (it.uppercase()) {
                          "BREAKFAST" -> DayPart.BREAKFAST
                          "LUNCH" -> DayPart.LUNCH
                          "DINNER" -> DayPart.DINNER
                          else -> throw IllegalArgumentException("Invalid day part")
                        }
                      }
              val result =
                      dySdk.engagements.reportImpression(
                              decisionId,
                              variations,
                              dayPartEnum,
                              branchId
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportClick") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val decisionId = config["decisionId"] as String
              val variation = config["variation"] as? Int
              val branchId = config["branchId"] as? String
              val dayPart = config["dayPart"] as? String

              val dayPartEnum =
                      dayPart?.let {
                        when (it.uppercase()) {
                          "BREAKFAST" -> DayPart.BREAKFAST
                          "LUNCH" -> DayPart.LUNCH
                          "DINNER" -> DayPart.DINNER
                          else -> throw IllegalArgumentException("Invalid day part")
                        }
                      }
              val result =
                      dySdk.engagements.reportClick(decisionId, variation, dayPartEnum, branchId)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportSlotClick") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val slotId = config["slotId"] as String
              val variation = config["variation"] as? Int
              val branchId = config["branchId"] as? String
              val dayPart = config["dayPart"] as? String

              val dayPartEnum =
                      dayPart?.let {
                        when (it.uppercase()) {
                          "BREAKFAST" -> DayPart.BREAKFAST
                          "LUNCH" -> DayPart.LUNCH
                          "DINNER" -> DayPart.DINNER
                          else -> throw IllegalArgumentException("Invalid day part")
                        }
                      }
              val result =
                      dySdk.engagements.reportSlotClick(variation, slotId, branchId, dayPartEnum)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportSlotImpression") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val slotsIds = config["slotsIds"] as List<String>
              val variation = config["variation"] as? Int
              val branchId = config["branchId"] as? String
              val dayPart = config["dayPart"] as? String

              val dayPartEnum =
                      dayPart?.let {
                        when (it.uppercase()) {
                          "BREAKFAST" -> DayPart.BREAKFAST
                          "LUNCH" -> DayPart.LUNCH
                          "DINNER" -> DayPart.DINNER
                          else -> throw IllegalArgumentException("Invalid day part")
                        }
                      }
              val result =
                      dySdk.engagements.reportSlotsImpression(
                              variation,
                              slotsIds,
                              branchId,
                              dayPartEnum
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    // Report events
    AsyncFunction("reportPurchaseEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val value = config["value"] as Double
              val cart = config["cart"] as List<Map<String, Any>>
              val currency = config["currency"] as? String
              val uniqueTransactionId = config["uniqueTransactionId"] as? String

              val cartItems =
                      cart.map { item ->
                        CartInnerItem(
                                productId = item["productId"] as String,
                                quantity = (item["quantity"] as Double).toUInt(),
                                itemPrice = (item["itemPrice"] as Double).toFloat()
                        )
                      }
              val currencyEnum = currency?.let { CurrencyType.valueOf(it.uppercase()) }
              val result =
                      dySdk.events.reportPurchaseEvent(
                              eventName,
                              value.toFloat(),
                              currencyEnum,
                              uniqueTransactionId,
                              cartItems
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportAddToCartEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val value = config["value"] as Double
              val quantity = config["quantity"] as Double
              val productId = config["productId"] as String
              val currency = config["currency"] as? String
              val cart = config["cart"] as? List<Map<String, Any>>

              val cartItems =
                      cart?.map { item ->
                        CartInnerItem(
                                productId = item["productId"] as String,
                                quantity = (item["quantity"] as Double).toUInt(),
                                itemPrice = (item["itemPrice"] as Double).toFloat()
                        )
                      }
              val currencyEnum = currency?.let { CurrencyType.valueOf(it.uppercase()) }
              val result =
                      dySdk.events.reportAddToCartEvent(
                              eventName,
                              value.toFloat(),
                              currencyEnum,
                              productId,
                              quantity.toUInt(),
                              cartItems
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportApplicationEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val value = config["value"] as Double
              val quantity = config["quantity"] as Double
              val productId = config["productId"] as String
              val currency = config["currency"] as? String
              val cart = config["cart"] as? List<Map<String, Any>>

              val cartItems =
                      cart?.map { item ->
                        CartInnerItem(
                                productId = item["productId"] as String,
                                quantity = (item["quantity"] as Double).toUInt(),
                                itemPrice = (item["itemPrice"] as Double).toFloat()
                        )
                      }
              val currencyEnum = currency?.let { CurrencyType.valueOf(it.uppercase()) }
              val result =
                      dySdk.events.reportApplicationEvent(
                              eventName,
                              value.toFloat(),
                              currencyEnum,
                              productId,
                              quantity.toUInt(),
                              cartItems
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportSubmissionEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val value = config["value"] as Double
              val cart = config["cart"] as List<Map<String, Any>>
              val currency = config["currency"] as? String

              val cartItems =
                      cart.map { item ->
                        CartInnerItem(
                                productId = item["productId"] as String,
                                quantity = (item["quantity"] as Double).toUInt(),
                                itemPrice = (item["itemPrice"] as Double).toFloat()
                        )
                      }
              val currencyEnum = currency?.let { CurrencyType.valueOf(it.uppercase()) }
              val result =
                      dySdk.events.reportSubmissionEvent(
                              eventName,
                              value.toFloat(),
                              currencyEnum,
                              cart = cartItems
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportSignUpEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val cuidType = config["cuidType"] as String
              val cuidTypeEnum = CuidType.valueOf(cuidType.uppercase())
              val result = dySdk.events.reportSignUpEvent(eventName, cuidTypeEnum.name)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportLoginEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val cuidType = config["cuidType"] as? String
              val cuid = config["cuid"] as? String
              val secondaryIdentifiers =
                      config["secondaryIdentifiers"] as? List<Map<String, String>>

              val cuidTypeEnum = cuidType?.let { CuidType.valueOf(it.uppercase()) }
              val secondaryIds =
                      secondaryIdentifiers?.map { identifier ->
                        SecondaryIdentifier(
                                type = identifier["type"] ?: "",
                                value = identifier["value"] ?: ""
                        )
                      }
              val result =
                      dySdk.events.reportLoginEvent(
                              eventName,
                              cuidTypeEnum?.name,
                              cuid,
                              secondaryIds
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportIdentifyUserEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val cuidType = config["cuidType"] as String
              val cuid = config["cuid"] as? String
              val secondaryIdentifiers =
                      config["secondaryIdentifiers"] as? List<Map<String, String>>

              val cuidTypeEnum = CuidType.valueOf(cuidType.uppercase())
              val secondaryIds =
                      secondaryIdentifiers?.map { identifier ->
                        SecondaryIdentifier(
                                type = identifier["type"] ?: "",
                                value = identifier["value"] ?: ""
                        )
                      }
              val result =
                      dySdk.events.reportIdentifyUserEvent(
                              eventName,
                              cuidTypeEnum.name,
                              cuid,
                              secondaryIds
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportNewsletterEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val cuidType = config["cuidType"] as? String
              val cuid = config["cuid"] as? String
              val secondaryIdentifiers =
                      config["secondaryIdentifiers"] as? List<Map<String, String>>

              val cuidTypeEnum = cuidType?.let { CuidType.valueOf(it.uppercase()) }
              val secondaryIds =
                      secondaryIdentifiers?.map { identifier ->
                        SecondaryIdentifier(
                                type = identifier["type"] ?: "",
                                value = identifier["value"] ?: ""
                        )
                      }
              val result =
                      dySdk.events.reportNewsletterEvent(
                              eventName,
                              cuidTypeEnum?.name,
                              cuid,
                              secondaryIds
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportRemoveFromCartEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val value = config["value"] as Double
              val quantity = config["quantity"] as Double
              val productId = config["productId"] as String
              val currency = config["currency"] as? String
              val cart = config["cart"] as? List<Map<String, Any>>

              val cartItems =
                      cart?.map { item ->
                        CartInnerItem(
                                productId = item["productId"] as String,
                                quantity = (item["quantity"] as Double).toUInt(),
                                itemPrice = (item["itemPrice"] as Double).toFloat()
                        )
                      }
              val currencyEnum = currency?.let { CurrencyType.valueOf(it.uppercase()) }
              val result =
                      dySdk.events.reportRemoveFromCartEvent(
                              eventName,
                              value.toFloat(),
                              currencyEnum,
                              productId,
                              quantity.toUInt(),
                              cartItems
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportSyncCartEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val value = config["value"] as Double
              val cart = config["cart"] as List<Map<String, Any>>
              val currency = config["currency"] as? String

              val cartItems =
                      cart.map { item ->
                        CartInnerItem(
                                productId = item["productId"] as String,
                                quantity = (item["quantity"] as Double).toUInt(),
                                itemPrice = (item["itemPrice"] as Double).toFloat()
                        )
                      }
              val currencyEnum = currency?.let { CurrencyType.valueOf(it.uppercase()) }
              val result =
                      dySdk.events.reportSyncCartEvent(
                              eventName,
                              value.toFloat(),
                              currencyEnum,
                              cartItems
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportMessageOptInEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val cuidType = config["cuidType"] as String
              val plainTextEmail = config["plainTextEmail"] as? String
              val externalId = config["externalId"] as? String

              val cuidTypeEnum = CuidType.valueOf(cuidType.uppercase())
              val result =
                      dySdk.events.reportMessageOptInEvent(
                              eventName,
                              cuidTypeEnum,
                              plainTextEmail,
                              externalId
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportMessageOptOutEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val cuidType = config["cuidType"] as String
              val plainTextEmail = config["plainTextEmail"] as? String
              val externalId = config["externalId"] as? String

              val cuidTypeEnum = CuidType.valueOf(cuidType.uppercase())
              val result =
                      dySdk.events.reportMessageOptOutEvent(
                              eventName,
                              cuidTypeEnum,
                              plainTextEmail,
                              externalId
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportInformAffinityEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val data = config["data"] as List<Map<String, Any>>
              val source = config["source"] as? String

              val affinityData =
                      data.map { item ->
                        InformAffinityData(
                                attribute = item["attribute"] as String,
                                values = (item["values"] as List<String>)
                        )
                      }
              val result = dySdk.events.reportInformAffinityEvent(eventName, source, affinityData)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportAddToWishListEvent") Coroutine
            { config: Map<String, Any> ->
              val eventName = config["eventName"] as String
              val productId = config["productId"] as String
              val size = config["size"] as? String
              val result =
                      DYSdk.getInstance()
                              .events
                              .reportAddToWishListEvent(eventName, productId, size)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportVideoWatchEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val itemId = config["itemId"] as String
              val autoplay = config["autoplay"] as Boolean
              val progress = config["progress"] as String
              val progressPercent = config["progressPercent"] as Double
              val categories = config["categories"] as? List<String>

              val progressEnum =
                      when (progress.uppercase()) {
                        "START" -> VideoProgressType.VIDEO_STARTED
                        "PROGRESS" -> VideoProgressType.VIDEO_PROGRESS
                        "COMPLETE" -> VideoProgressType.VIDEO_FINISHED
                        else -> throw IllegalArgumentException("Invalid progress type")
                      }
              val result =
                      dySdk.events.reportVideoWatchEvent(
                              eventName,
                              itemId,
                              categories,
                              autoplay,
                              progressEnum,
                              progressPercent.toUInt()
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportKeywordSearchEvent") Coroutine
            { config: Map<String, Any> ->
              val eventName = config["eventName"] as String
              val keywords = config["keywords"] as String
              val result = DYSdk.getInstance().events.reportKeywordSearchEvent(eventName, keywords)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportChangeAttributesEvent") Coroutine
            { config: Map<String, Any> ->
              val eventName = config["eventName"] as String
              val attributeType = config["attributeType"] as String
              val attributeValue = config["attributeValue"] as String
              val result =
                      DYSdk.getInstance()
                              .events
                              .reportChangeAttributesEvent(eventName, attributeType, attributeValue)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportFilterItemsEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val filterType = config["filterType"] as String
              val filterNumericValue = config["filterNumericValue"] as? Int
              val filterStringValue = config["filterStringValue"] as? String
              val result =
                      dySdk.events.reportFilterItemsEvent(
                              eventName,
                              filterType,
                              filterNumericValue,
                              filterStringValue
                      )
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportSortItemsEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val sortBy = config["sortBy"] as String
              val sortOrder = config["sortOrder"] as String

              val sortOrderEnum =
                      when (sortOrder.uppercase()) {
                        "ASC" -> SortOrderType.ASC
                        "DESC" -> SortOrderType.DESC
                        else -> throw IllegalArgumentException("Invalid sort order")
                      }
              val result = dySdk.events.reportSortItemsEvent(eventName, sortBy, sortOrderEnum)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    AsyncFunction("reportPromoCodeEnterEvent") Coroutine
            { config: Map<String, Any> ->
              val eventName = config["eventName"] as String
              val code = config["code"] as String
              val result = DYSdk.getInstance().events.reportPromoCodeEnterEvent(eventName, code)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    // Define a subclass of CustomProperties that holds a map of properties
    class DynamicCustomProperties(val map: Map<String, Any>) : CustomProperties()

    AsyncFunction("reportCustomEvent") Coroutine
            { config: Map<String, Any> ->
              val dySdk = DYSdk.getInstance()
              val eventName = config["eventName"] as String
              val properties = config["properties"] as Map<String, Any>
              val customProperties = DynamicCustomProperties(properties)
              val result = dySdk.events.reportCustomEvents(eventName, customProperties)
              when (result.status) {
                ResultStatus.SUCCESS -> {
                  mapOf("status" to "success")
                }
                ResultStatus.WARNING -> {
                  result.warnings?.let { warnings ->
                    mapOf("status" to "warning", "warnings" to warnings)
                  }
                }
                ResultStatus.ERROR -> {
                  result.error?.let {
                    mapOf(
                            "status" to "error",
                            "error" to result.rawNetworkData?.body,
                            "message" to result.rawNetworkData?.message,
                            "code" to result.rawNetworkData?.code
                    )
                  }
                }
              }
            }

    // Set log level
    Function("setLogLevel") { config: Map<String, Any> ->
      val dySdk = DYSdk.getInstance()
      val logLevel = config["logLevel"] as String
      val logLevelEnum =
              when (logLevel.uppercase()) {
                "VERBOSE" -> LogLevel.VERBOSE
                "DEBUG" -> LogLevel.DEBUG
                "INFO" -> LogLevel.INFO
                "WARN" -> LogLevel.WARN
                "ERROR" -> LogLevel.ERROR
                "NONE" -> LogLevel.OFF
                else -> throw IllegalArgumentException("Invalid log level")
              }
      dySdk.setLogLevel(logLevelEnum)
    }

    // Reset User ID and Session ID
    Function("resetUserIdAndSessionId") { DYSdk.getInstance().resetUserIdAndSessionId() }
  }
}
