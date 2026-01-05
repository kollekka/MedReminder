package com.elozelo.medreminder

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


data class NavPage(var nameResId: Int, var route: String, var icon: ImageVector)

object NavRoutes {
    val Home = NavPage(R.string.nav_home, "home", Icons.Filled.Home)
    val Medications = NavPage(R.string.nav_medications, "medications", Icons.Filled.Healing)
    val Reminders = NavPage(R.string.nav_appointments, "Appointments", Icons.Filled.Event)
    val Settings = NavPage(R.string.nav_settings, "settings", Icons.Filled.Settings)

    val pages = listOf(Home, Medications, Reminders, Settings)
}


@Composable
private fun NavBarItem(page: NavPage, modifier: Modifier = Modifier, selected: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 12.dp)
    ) {
        Image(imageVector = page.icon,
            contentDescription = stringResource(id = page.nameResId),
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp),
            colorFilter = ColorFilter.tint(
                if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary
            ),
        )
        Text(
            text = stringResource(id = page.nameResId),
            fontSize = 14.sp,
            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary
        )
    }
}


@Composable
fun NavBar(selectedRoute: String = NavRoutes.Home.route, onChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .navigationBarsPadding()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            for (page in NavRoutes.pages) {
                NavBarItem(
                    page = page,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clickable {
                            onChange(page.route)
                        },
                    selected = selectedRoute == page.route
                )
            }
        }
    }
}