package ru.hh.plugins.geminio.sdk

import com.intellij.openapi.project.Project
import ru.hh.plugins.geminio.sdk.models.GeminioTemplateData
import ru.hh.plugins.geminio.sdk.recipe.models.GeminioRecipe

/**
 * SDK with Geminio functionality.
 */
interface GeminioSdk {

    fun parseYamlRecipe(recipeFilePath: String): GeminioRecipe

    fun createGeminioTemplateData(project: Project, geminioRecipe: GeminioRecipe): GeminioTemplateData
}
