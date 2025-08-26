package com.example.financeapp.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme

data class CategoryUi(
    val icon: ImageVector,
    val container: Color,          // solid chip/circle background
    val onContainer: Color,        // icon/text on top of container
    val chipContainer: Color,      // subtle chip bg
    val chipLabel: Color,          // label color on subtle chip
    val tintedCard: Color          // card tint
)

@Composable
fun rememberCategoryUi(category: String): CategoryUi {
    val c = MaterialTheme.colorScheme
    fun pack(icon: ImageVector, container: Color): CategoryUi {
        return CategoryUi(
            icon = icon,
            container = container,
            onContainer = c.onSecondaryContainer,
            chipContainer = container.copy(alpha = 0.25f),
            chipLabel = c.onSurfaceVariant,
            tintedCard = container.copy(alpha = 0.15f)
        )
    }
    return when (category.lowercase()) {
        "shopping"      -> pack(Icons.Outlined.ShoppingCart, c.secondaryContainer)
        "groceries"     -> pack(Icons.Outlined.ShoppingCart, c.tertiaryContainer)
        "food", "dining", "restaurants"
            -> pack(Icons.Outlined.Fastfood, c.tertiaryContainer)
        "transport", "transportation", "car", "taxi", "fuel"
            -> pack(Icons.Outlined.DirectionsCar, c.primaryContainer)
        "bills", "utilities"
            -> pack(Icons.Outlined.ReceiptLong, c.secondaryContainer)
        "entertainment", "movies", "fun"
            -> pack(Icons.Outlined.Movie, c.tertiaryContainer)
        "health", "medical", "pharmacy"
            -> pack(Icons.Outlined.HealthAndSafety, c.primaryContainer)
        "salary", "income", "payroll"
            -> pack(Icons.Outlined.AttachMoney, c.secondaryContainer)
        "recurring"      -> pack(Icons.Outlined.ReceiptLong, c.secondaryContainer)
        else             -> pack(Icons.Outlined.Category, c.surfaceVariant)
    }
}
