#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'flutter_kin_ecosystem_sdk'
  s.version          = '0.0.1'
  s.summary          = 'A flutter Kin Ecosystem SDK plugin to use offers features and launch Kin Marketplace.'
  s.description      = <<-DESC
A flutter Kin Ecosystem SDK plugin to use offers features and launch Kin Marketplace.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.dependency 'KinDevPlatform', '1.0.6'
  s.dependency 'KinCoreSDK'
  s.static_framework = true
  s.ios.deployment_target = '9.0'
end

