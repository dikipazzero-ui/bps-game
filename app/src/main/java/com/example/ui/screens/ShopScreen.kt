package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AttributeCatalog
import com.example.model.ShopCategory
import com.example.model.UniformCatalog
import com.example.model.UserProfile
import com.example.model.WeaponCatalog
import com.example.ui.components.AttributeVisualBadge
import com.example.ui.components.Character3DPreviewStage
import com.example.ui.components.UniformVisualBadge
import com.example.ui.components.WeaponVisualBadge
import com.example.viewmodel.UiState

@Composable
fun ShopScreen(
    profileState: UiState<UserProfile>,
    isBusy: Boolean,
    onPurchaseOrEquipUniform: (String) -> Unit,
    onPurchaseOrEquipWeapon: (String) -> Unit,
    onPurchaseOrEquipAttribute: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val profile = (profileState as? UiState.Success)?.data
    val points = profile?.points ?: 0L
    val unlockedUniforms = profile?.unlockedUniformIds ?: listOf("rookie_cadet")
    val equippedUniformId = profile?.equippedUniformId ?: "rookie_cadet"
    val unlockedWeapons = profile?.unlockedWeaponIds ?: listOf("pistol_9mm")
    val equippedWeaponId = WeaponCatalog.byId(profile?.equippedWeaponId ?: "pistol_9mm").id
    val unlockedAttributes = profile?.unlockedAttributeIds ?: listOf("utility_belt")
    val equippedAttributeId = profile?.equippedAttributeId ?: "utility_belt"

    var selectedCategory by remember { mutableStateOf(ShopCategory.UNIFORM) }
    var previewUniformId by remember(equippedUniformId) { mutableStateOf(equippedUniformId) }
    var previewWeaponId by remember(equippedWeaponId) { mutableStateOf(equippedWeaponId) }
    var previewAttributeId by remember(equippedAttributeId) { mutableStateOf(equippedAttributeId) }

    val previewUniform = UniformCatalog.byId(previewUniformId)
    val previewWeapon = WeaponCatalog.byId(previewWeaponId)
    val previewAttribute = AttributeCatalog.byId(previewAttributeId)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Studio Pratinjau Karakter 3D Interaktif
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GUDANG PERLENGKAPAN 3D",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Geser model 3D untuk memutar • Ketuk item untuk coba visual",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Stars,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "$points POIN",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, previewUniform.trimColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            ) {
                                Character3DPreviewStage(
                                    uniform = previewUniform,
                                    weapon = previewWeapon,
                                    attribute = previewAttribute,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color(0xAA090E1A),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "PRATINJAU 3D",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF22D3EE),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Kebutuhan Dalam Game / Live Stats
                            Column(
                                modifier = Modifier.weight(0.95f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "KEBUTUHAN TEMPUR",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                NeedStatRow(
                                    label = "Proteksi Armor (Lv.${previewUniform.armorLevel})",
                                    progress = (previewUniform.damageReduction / 0.75f).coerceIn(0.15f, 1f),
                                    valueText = "-${(previewUniform.damageReduction * 100).toInt()}% DMG",
                                    color = Color(0xFF10B981)
                                )
                                NeedStatRow(
                                    label = "Daya Serang Senjata",
                                    progress = (previewWeapon.damage / 95f).coerceIn(0.22f, 1f),
                                    valueText = "${previewWeapon.damage} DMG",
                                    color = Color(0xFFF59E0B)
                                )
                                NeedStatRow(
                                    label = "Kecepatan Lari",
                                    progress = ((previewAttribute.speedMultiplier - 0.8f) / 0.7f).coerceIn(0.25f, 1f),
                                    valueText = "${(previewAttribute.speedMultiplier * 100).toInt()}%",
                                    color = Color(0xFF38BDF8)
                                )
                                NeedStatRow(
                                    label = "Visibilitas Malam",
                                    progress = (0.25f + previewAttribute.visionBoost * 0.75f).coerceIn(0.25f, 1f),
                                    valueText = if (previewAttribute.visionBoost > 0.5f) "NVG" else "Normal",
                                    color = Color(0xFF34D399)
                                )
                                Text(
                                    text = "• Baju: ${previewUniform.name}\n• Senjata: ${previewWeapon.name}\n• Atribut: ${previewAttribute.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Tab Kategori Belanja (Baju, Senjata, Atribut)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShopCategory.entries.forEach { cat ->
                        val icon = when (cat) {
                            ShopCategory.UNIFORM -> Icons.Filled.Checkroom
                            ShopCategory.WEAPON -> Icons.Filled.Bolt
                            ShopCategory.ATTRIBUTE -> Icons.Filled.Security
                        }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shop_tab_${cat.name}")
                        )
                    }
                }
            }

            // 3. Daftar Item Sesuai Kategori
            when (selectedCategory) {
                ShopCategory.UNIFORM -> {
                    items(UniformCatalog.items, key = { it.id }) { uniform ->
                        val isUnlocked = uniform.id in unlockedUniforms
                        val isEquipped = uniform.id == equippedUniformId
                        val isPreviewing = uniform.id == previewUniformId
                        val canAfford = points >= uniform.price

                        ShopItemCard(
                            title = uniform.name,
                            subtitle = "${uniform.rankTitle} • ${uniform.bonusFlair}",
                            description = uniform.description,
                            accentColor = uniform.primaryColor,
                            price = uniform.price,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            isPreviewing = isPreviewing,
                            canAfford = canAfford,
                            isBusy = isBusy,
                            actionTag = if (isUnlocked) "equip_${uniform.id}" else "buy_${uniform.id}",
                            onPreview = { previewUniformId = uniform.id },
                            onAction = {
                                previewUniformId = uniform.id
                                onPurchaseOrEquipUniform(uniform.id)
                            },
                            visual = { UniformVisualBadge(uniform) }
                        )
                    }
                }
                ShopCategory.WEAPON -> {
                    items(WeaponCatalog.items, key = { it.id }) { weapon ->
                        val isUnlocked = weapon.id in unlockedWeapons
                        val isEquipped = weapon.id == equippedWeaponId
                        val isPreviewing = weapon.id == previewWeaponId
                        val canAfford = points >= weapon.price

                        ShopItemCard(
                            title = weapon.name,
                            subtitle = "${weapon.categoryTag} • ${weapon.damage} DMG" +
                                (if (weapon.aoeRadius > 0f) " (Area)" else "") +
                                (if (weapon.bonusPoints > 0) " • +${weapon.bonusPoints} Poin" else ""),
                            description = weapon.description,
                            accentColor = weapon.accentColor,
                            price = weapon.price,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            isPreviewing = isPreviewing,
                            canAfford = canAfford,
                            isBusy = isBusy,
                            actionTag = if (isUnlocked) "equip_wpn_${weapon.id}" else "buy_wpn_${weapon.id}",
                            onPreview = { previewWeaponId = weapon.id },
                            onAction = {
                                previewWeaponId = weapon.id
                                onPurchaseOrEquipWeapon(weapon.id)
                            },
                            visual = { WeaponVisualBadge(weapon) }
                        )
                    }
                }
                ShopCategory.ATTRIBUTE -> {
                    items(AttributeCatalog.items, key = { it.id }) { attr ->
                        val isUnlocked = attr.id in unlockedAttributes
                        val isEquipped = attr.id == equippedAttributeId
                        val isPreviewing = attr.id == previewAttributeId
                        val canAfford = points >= attr.price

                        ShopItemCard(
                            title = attr.name,
                            subtitle = attr.needLabel,
                            description = attr.description,
                            accentColor = attr.accentColor,
                            price = attr.price,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            isPreviewing = isPreviewing,
                            canAfford = canAfford,
                            isBusy = isBusy,
                            actionTag = if (isUnlocked) "equip_attr_${attr.id}" else "buy_attr_${attr.id}",
                            onPreview = { previewAttributeId = attr.id },
                            onAction = {
                                previewAttributeId = attr.id
                                onPurchaseOrEquipAttribute(attr.id)
                            },
                            visual = { AttributeVisualBadge(attr) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeedStatRow(
    label: String,
    progress: Float,
    valueText: String,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun ShopItemCard(
    title: String,
    subtitle: String,
    description: String,
    accentColor: Color,
    price: Long,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    isPreviewing: Boolean,
    canAfford: Boolean,
    isBusy: Boolean,
    actionTag: String,
    onPreview: () -> Unit,
    onAction: () -> Unit,
    visual: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isEquipped || isPreviewing) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
        ),
        border = BorderStroke(
            width = if (isEquipped) 2.dp else if (isPreviewing) 1.5.dp else 1.dp,
            color = if (isEquipped) accentColor else if (isPreviewing) Color(0xFF22D3EE) else MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPreview)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            visual()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isPreviewing && !isEquipped) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Visibility,
                            contentDescription = "Sedang Dipratinjau",
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            when {
                isEquipped -> {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Dipakai",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("AKTIF", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                isUnlocked -> {
                    OutlinedButton(
                        onClick = onAction,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag(actionTag)
                    ) {
                        Text("PAKAI")
                    }
                }
                else -> {
                    Button(
                        onClick = onAction,
                        enabled = !isBusy && canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag(actionTag)
                    ) {
                        if (!canAfford) {
                            Icon(Icons.Filled.Lock, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text("$price")
                    }
                }
            }
        }
    }
}
