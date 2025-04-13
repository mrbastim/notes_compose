package com.example.notes

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavHostController
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import java.util.concurrent.CompletableFuture

class NoteActivity(private val navController: NavHostController, private val noteId: String) {
    private val title = mutableStateOf("")
    private val text = mutableStateOf("")
    private val json = jacksonObjectMapper()
    val api = APISender()

    init {
        if (noteId != "-1") {
            GlobalScope.launch(Dispatchers.IO) {
                val noteFuture: CompletableFuture<Response> = api.get("/GetNoteById?id=$noteId")
                val noteResponse = noteFuture.get()
                if (noteResponse.isSuccessful) {
                    val note = json.readValue<NoteViewModel>(noteResponse.body?.string().toString())
                    noteResponse.close()
                    withContext(Dispatchers.Main) {
                        title.value = note.Title
                        text.value = note.Text
                    }
                }
            }
        }
    }

    @Composable
    fun NoteActivityScreen() {
        // Код интерфейса, который отображает и позволяет изменять заголовок и текст заметки
        // Например, используйте TextField для ввода заголовка и текста заметки
        // и Button для сохранения изменений
        // Здесь вы можете использовать title.value и text.value для доступа к заголовку и тексту заметки
        // Например:
         TextField(value = title.value, onValueChange = { title.value = it }, label = { Text("Заголовок") })
         TextField(value = text.value, onValueChange = { text.value = it }, label = { Text("Текст") })
         Button(onClick = { saveNote() }) {
             Text("Сохранить")
         }
    }
}