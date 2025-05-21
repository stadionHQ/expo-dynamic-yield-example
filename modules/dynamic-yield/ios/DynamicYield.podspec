Pod::Spec.new do |s|
  s.name           = 'DynamicYield'
  s.version        = '1.0.0'
  s.summary        = 'Dynamic Yield SDK for iOS'
  s.description    = 'Dynamic Yield SDK for iOS'
  s.author         = 'Anthony Lasserre'
  s.homepage       = 'https://docs.expo.dev/modules/'
  s.platforms      = { :ios => '14.0' }
  s.source         = { git: '' }
  s.static_framework = true

	s.dependency 'ExpoModulesCore'

  # Swift/Objective-C compatibility
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'SWIFT_COMPILATION_MODE' => 'wholemodule'
  }

  # Add SPM dependency
  if defined?(:spm_dependency)
    spm_dependency(s,  
    url: 'https://github.com/DynamicYield/Dynamic-Yield-Mobile-SDK-Swift', 
    requirement: {kind: 'upToNextMajorVersion', minimumVersion: '1.0.0'}, 
    products: ['DyLibrary'] 
  ) 
  else 
    raise "Please upgrade React Native to >=0.75.0 to use SPM dependencies." 
  end 

  s.source_files = "**/*.{h,m,mm,swift,hpp,cpp}"
end
