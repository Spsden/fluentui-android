package com.microsoft.fluentui.generator

import java.io.File

const val YAML_PATH = "../FluentUI/FluentUIStyleSheets"
const val YAML_STYLESHEET_SUFFIX = "Style.yml"

var global_projectRoot = "../FluentUI"
    set(value) {
        field = value
        global_projectSourcePath = "$global_projectRoot/generatedSrc/"
    }
var global_projectSourcePath: String = "$global_projectRoot/generatedSrc/"

lateinit var global_flavorName: String
    private set
lateinit var global_flavorPath: String
    private set

fun main(args: Array<String>) {
    when {
        args.isEmpty() -> printError("No arguments specified")
        args[0] == "generate" -> {
            if (args.size == 1) {
                // Generate from all local resources
                Generator().apply {
                    addAll(getLocalStyleSheetList())
                    generate()
                }
            } else {
                Generator().apply {
                    add(StylesheetDocument("$YAML_PATH/GenericStyle.yml", DocumentType.ROOT))

                    if (args.size > 1) add(StylesheetDocument(args[1], DocumentType.REGULAR))
                    if (args.size > 2) add(StylesheetDocument(args[2], DocumentType.DARKTHEME))

                    generate()
                }
            }
        }
        args[0] == "clean" -> { cleaner() }
        else -> printError("Unknown argument: ${args[0]}")
    }
}


fun configFlavor(name: String) {
    global_flavorName = name
    global_flavorPath = global_projectSourcePath.plus(global_flavorName).plus("/")
    if (name != "main") upgradeGradleFlavors()
}

fun getLocalStyleSheetList(): List<StylesheetDocument> {
    val docs = mutableListOf<StylesheetDocument>()

    File(YAML_PATH).list()?.apply {
        filter { it.endsWith(YAML_STYLESHEET_SUFFIX) }.sortedWith(Comparator<String> { name, name2 ->
            when {
                name.startsWith("Generic") -> -1
                else -> name.compareTo(name2)
            }
        }).forEach {
            val filename = it.substringBefore(YAML_STYLESHEET_SUFFIX).toLowerCase()
            val type = when {
                filename.equals("generic") -> DocumentType.ROOT
                filename.endsWith("dark") -> DocumentType.DARKTHEME
                else -> DocumentType.REGULAR
            }

            docs.add(StylesheetDocument("$YAML_PATH/$it", type))
        }
    } ?: printError("No stylesheet files found!")

    return docs
}

fun upgradeGradleFlavors() {
    val generatedFolder = File("$global_projectRoot/generatedSrc")
    val flavorsFile = File(generatedFolder, "flavors.gradle")

    if (!generatedFolder.exists() || !flavorsFile.exists()) {
        generatedFolder.mkdirs()
        flavorsFile.createNewFile()
        flavorsFile.writeText("android.productFlavors {\n    }")
    }
    val buildFileContent = flavorsFile.readText()
    Regex("android.productFlavors \\{([^}]*)}").run {
        find(buildFileContent)?.value?.let { flavorsString ->
            if (!flavorsString.contains(global_flavorName)) {
                replace(
                    buildFileContent,
                    flavorsString.replace("}", "    $global_flavorName\n    }")
                )
            } else null
        }
    }?.also {
        flavorsFile.writeText(it)
        print("> Flavor \"$global_flavorName\" added to flavors.gradle")
    }
}