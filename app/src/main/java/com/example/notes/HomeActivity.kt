package com.example.notes

import android.annotation.SuppressLint
import android.os.StrictMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.PreviewActivity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture



@Preview
@Composable
fun PreviewHomeActivityScreen() {
    val navController = rememberNavController()
    HomeActivity(navController).HomeActivityScreen()
}


@SuppressLint("NewApi")
class HomeActivity(private val navController: NavHostController) {
    private val json = jacksonObjectMapper()
    val api = APISender()
    private val notes = mutableStateOf(listOf<NoteViewModel>())
    private val isNotesUploaded = mutableStateOf(false)

    init {
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch(Dispatchers.IO) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            val notesFuture: CompletableFuture<Response> = api.get("/GetAllNotes")
            val notesResponse = notesFuture.get()
            if (notesResponse.isSuccessful) {
                val notesList = json.readValue<List<NoteViewModel>>(notesResponse.body?.string().toString())
                notesResponse.close()
                val sortedNotes = notesList.sortedByDescending { ZonedDateTime.parse(it.UpdatedAt) }
                withContext(Dispatchers.Main) {
                    notes.value = sortedNotes
                    isNotesUploaded.value = true
                }
            }
        }
    }

    @Composable
    fun HomeActivityScreen() {
        val notesList by remember { notes }
        val isNotesUploaded by remember { isNotesUploaded }
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 16.dp, top = 40.dp)
            ) {
                Text(text = "Заметки", fontSize = 25.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(20.dp))
                if (isNotesUploaded && notesList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = rememberLazyListState()
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                        items(items = notesList) { note ->
                            NoteItem(note)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else {
                    Text(text = "Загрузка заметок...")
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(50.dp)
                    .background(Color.Black, shape = RoundedCornerShape(25.dp))
                    .clickable {
                        navController.navigate(Destinations.NoteScreen.route + "/-1") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            restoreState = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = Color.White, fontSize = 25.sp)
            }
        }
    }

    @Composable
    fun NoteItem(note: NoteViewModel) {
        Box(
            modifier = Modifier
                .height(100.dp)
                .background(Color(0xFFF5F4F2), shape = RoundedCornerShape(8.dp))
                .padding(vertical = 7.dp, horizontal = 10.dp)
                .clickable {
                    navController.navigate(Destinations.NoteScreen.route + "/${note.Id}") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        restoreState = true
                    }
                }
        ) {
            // Содержимое заметки
        }
    }


}