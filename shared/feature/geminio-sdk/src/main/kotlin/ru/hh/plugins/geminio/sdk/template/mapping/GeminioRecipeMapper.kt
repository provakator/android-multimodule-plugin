package ru.hh.plugins.geminio.sdk.template.mapping

import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.template
import com.intellij.openapi.project.Project
import ru.hh.plugins.freemarker_wrapper.FreemarkerConfiguration
import ru.hh.plugins.geminio.sdk.GeminioAdditionalParamsStore
import ru.hh.plugins.geminio.sdk.GeminioSdkConstants
import ru.hh.plugins.geminio.sdk.models.GeminioTemplateData
import ru.hh.plugins.geminio.sdk.models.GeminioTemplateParametersIds
import ru.hh.plugins.geminio.sdk.recipe.models.GeminioRecipe
import ru.hh.plugins.geminio.sdk.template.aliases.AndroidStudioTemplateParameter
import ru.hh.plugins.geminio.sdk.template.aliases.AndroidStudioTemplateStringParameter
import ru.hh.plugins.geminio.sdk.template.executors.executeGeminioRecipe
import ru.hh.plugins.geminio.sdk.template.mapping.optional.injectOptionalParams
import ru.hh.plugins.geminio.sdk.template.mapping.required.injectRequiredParams
import ru.hh.plugins.geminio.sdk.template.mapping.widgets.injectWidgets
import ru.hh.plugins.geminio.sdk.template.models.GeminioRecipeExecutorData

/**
 * Mapping from [ru.hh.plugins.geminio.sdk.recipe.models.GeminioRecipe]
 * into [ru.hh.plugins.geminio.sdk.models.GeminioTemplateData].
 */
internal fun GeminioRecipe.toGeminioTemplateData(project: Project): GeminioTemplateData {
    val geminioRecipe = this

    val existingParametersMap = mutableMapOf<String, AndroidStudioTemplateParameter>()
    val paramsStore = GeminioAdditionalParamsStore()
    val androidStudioTemplate = template {
        injectRequiredParams(geminioRecipe)
        injectOptionalParams(geminioRecipe)

        existingParametersMap += injectWidgets(geminioRecipe)

        var isDryRun = true
        recipe = { templateData ->
            val moduleTemplateData = templateData as ModuleTemplateData
            executeGeminioRecipe(
                geminioRecipe = geminioRecipe,
                executorData = GeminioRecipeExecutorData(
                    project = project,
                    isDryRun = isDryRun,
                    moduleTemplateData = moduleTemplateData,
                    existingParametersMap = existingParametersMap,
                    resolvedParamsMap = existingParametersMap.asIterable().associate { entry ->
                        entry.key to entry.value.value
                    }.plus(
                        getHardcodedParamsMap(moduleTemplateData, existingParametersMap)
                    ).plus(
                        paramsStore
                    ),
                    freemarkerConfiguration = FreemarkerConfiguration(geminioRecipe.freemarkerTemplatesRootDirPath)
                )
            )
            isDryRun = false
        }
    }

    return GeminioTemplateData(
        existingParametersMap = existingParametersMap,
        androidStudioTemplate = androidStudioTemplate,
        geminioIds = GeminioTemplateParametersIds(
            newModuleNameParameterId = GeminioSdkConstants.FEATURE_MODULE_NAME_PARAMETER_ID,
            newModulePackageNameParameterId = GeminioSdkConstants.FEATURE_PACKAGE_NAME_PARAMETER_ID,
            newApplicationModulesParameterId = GeminioSdkConstants.FEATURE_APPLICATIONS_MODULES_PARAMETER_ID,
        ),
        paramsStore = paramsStore
    )
}

private fun getHardcodedParamsMap(
    moduleTemplateData: ModuleTemplateData,
    existingParametersMap: Map<String, AndroidStudioTemplateParameter>
): Map<String, Any?> {
    val packageNameParameter = existingParametersMap[GeminioSdkConstants.FEATURE_PACKAGE_NAME_PARAMETER_ID]
        as? AndroidStudioTemplateStringParameter

    val (packageName, applicationPackage) = when {
        packageNameParameter != null -> {
            Pair(packageNameParameter.value, packageNameParameter.value)
        }

        else -> {
            Pair(moduleTemplateData.packageName, moduleTemplateData.projectTemplateData.applicationPackage)
        }
    }

    return mapOf(
        HardcodedParams.PACKAGE_NAME to packageName,
        HardcodedParams.APPLICATION_PACKAGE to applicationPackage,
    )
}

private object HardcodedParams {
    /**
     * Package name from SELECTED file.
     */
    const val PACKAGE_NAME = "packageName"

    /**
     * Package name from current gradle module.
     */
    const val APPLICATION_PACKAGE = "applicationPackage"
}
