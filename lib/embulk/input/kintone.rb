Embulk::JavaPlugin.register_input(
  "kintone", "org.embulk.input.kintone.KintoneInputPlugin",
  File.expand_path('../../../../classpath', __FILE__))
