package com.example.notes

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notes.ui.theme.NotesTheme


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotesTheme {
                val navController: NavHostController = rememberNavController()
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background)
                {
                    Box(modifier = Modifier.background(Color.White))
                    {
                        NavigationGraph(navController)
                    }
                }

            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun NavigationGraph(navController: NavHostController)
    {
        NavHost(navController, startDestination = Destinations.HomeScreen.route)
        {
            composable(Destinations.HomeScreen.route)
            {
                isNoteInitialize.value = false
                if(!isHomeInitialize.value)
                {
                    isHomeInitialize.value = true
                    home = HomeActivity(navController)
                }
                home.HomeActivityScreen() // будет подсвечиваться красным, это нормально
            }
            composable(Destinations.NoteScreen.route + "/{note_id}", arguments = listOf(navArgument("note_id"){ type = NavType.StringType }))
            {
                isHomeInitialize.value = false
                if(!isNoteInitialize.value)
                {
                    isNoteInitialize.value = true
                    note = NoteActivity(navController, it.arguments?.getString("note_id")!!)
                }
                note.NoteActivityScreen() // будет подсвечиваться красным, это нормально
            }
        }
    }


}

private val isHomeInitialize = mutableStateOf(false)
private val isNoteInitialize = mutableStateOf(false)
private lateinit var home : HomeActivity
private lateinit var note : NoteActivity

