package com.example.notesappcompose

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check // Иконка сохранения
import androidx.compose.material3.* // TopAppBar, IconButton, Icon, etc.
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.notes.APISender
import com.example.notes.Destinations
import com.example.notes.NoteViewModel
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.*
import okhttp3.Response
import java.util.concurrent.CompletableFuture

@OptIn(ExperimentalMaterial3Api::class) // Для Scaffold и TopAppBar
class NoteActivity(
    private val navController: NavHostController,
    private val noteId: String // ID заметки ("-1" для новой)
) {
    private val isNewNote = noteId == "-1"

    // API и JSON
    private val json = jacksonObjectMapper()
    private val api = APISender()

    // Состояние для заголовка и текста (стр. 27, 33)
    val title = mutableStateOf("")
    val text = mutableStateOf("")

    // Состояние загрузки
    private val isLoading = mutableStateOf(!isNewNote) // Загрузка, если это не новая заметка

    init {
        if (!isNewNote) {
            loadNoteDetails()
        }
    }

    @SuppressLint("NewApi")
    private fun loadNoteDetails() {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Запрос на получение деталей заметки по ID (стр. 27)
                // Убедитесь, что ваш API имеет эндпоинт GetNoteById?id=...
                val noteFuture: CompletableFuture<Response> = api.get("/GetNoteById?id=$noteId")
                val noteResponse = noteFuture.get()

                if (noteResponse.isSuccessful) {
                    val body = noteResponse.body?.string()
                    if (body != null) {
                        val noteDetails: NoteViewModel = json.readValue(body)
                        withContext(Dispatchers.Main) {
                            title.value = noteDetails.Title
                            text.value = noteDetails.Text
                            isLoading.value = false // Загрузка завершена
                        }
                    } else {
                        withContext(Dispatchers.Main) { isLoading.value = false }
                    }
                } else {
                    withContext(Dispatchers.Main) { isLoading.value = false }
                }
                noteResponse.close()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isLoading.value = false }
            }
        }
    }

    // Функция сохранения заметки (стр. 32)
    @SuppressLint("NewApi")
    private fun saveNote() {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Формируем JSON тело запроса для POST (если API требует)
                // val noteData = mapOf("title" to title.value, "text" to text.value)
                // val jsonBody = json.writeValueAsString(noteData)
                // Если API принимает параметры в URL:

                val urlParams = "title=${UriEscape.encode(title.value)}&text=${UriEscape.encode(text.value)}"

                val saveFuture: CompletableFuture<String> // Ожидаем ID или статус в ответе

                if (isNewNote) {
                    // Запрос на создание новой заметки (стр. 32)
                    // Убедитесь, что API имеет эндпоинт CreateNote?title=...&text=...
                    saveFuture = api.post("/CreateNote?$urlParams")
                } else {
                    // Запрос на редактирование существующей заметки (стр. 32)
                    // Убедитесь, что API имеет эндпоинт EditNote?id=...&title=...&text=...
                    saveFuture = api.post("/EditNote?id=$noteId&$urlParams")
                }

                val result = saveFuture.get() // Ожидаем ответ (например, ID заметки)
                println("Save result: $result") // Логируем результат

                // После успешного сохранения возвращаемся на главный экран
                withContext(Dispatchers.Main) {
                    navController.navigateUp() // Возврат назад (стр. 32)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Здесь можно показать сообщение об ошибке сохранения
            }
        }
    }


    // Composable функция для экрана заметки
    @Composable
    fun NoteActivityScreen() {
        val currentTitle by remember { title }
        val currentText by remember { text }
        val loading by remember { isLoading }

        // Scaffold предоставляет базовую структуру Material Design
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(Destinations.NoteScreen.title ?: "Заметка") },
                    navigationIcon = { // Кнопка "назад"
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = { // Кнопка "сохранить"
                        IconButton(onClick = { saveNote() }) {
                            Icon(Icons.Filled.Check, contentDescription = "Сохранить")
                        }
                    }
                )
            }
        ) { paddingValues -> // paddingValues содержит отступы от TopAppBar

            if (loading) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Колонка с возможностью прокрутки для полей ввода
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues) // Применяем отступы
                        .padding(16.dp) // Дополнительные отступы по краям
                        .verticalScroll(rememberScrollState()) // Делаем колонку скроллящейся
                ) {
                    // Поле для заголовка (стр. 33)
                    BasicTextField(
                        value = currentTitle,
                        onValueChange = { newText -> title.value = newText },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            color = Color(0xFF404040) // Цвет из примера
                        ),
                        singleLine = true, // Заголовок обычно однострочный
                        decorationBox = { innerTextField -> // Плейсхолдер (стр. 33)
                            Box(modifier = Modifier.padding(vertical = 8.dp)) { // Отступы для плейсхолдера/текста
                                if (currentTitle.isEmpty()) {
                                    Text(
                                        text = "Введите заголовок",
                                        fontSize = 20.sp,
                                        color = Color(0xFF9C9C9C) // Цвет плейсхолдера
                                    )
                                }
                                innerTextField() // Отображение самого поля ввода
                            }
                        },
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary) // Цвет курсора
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Поле для текста заметки (адаптируем пример со стр. 33)
                    BasicTextField(
                        value = currentText,
                        onValueChange = { newText -> text.value = newText },
                        // modifier = Modifier.fillMaxWidth().weight(1f), // Занимает оставшееся место
                        // Или используем heightIn для минимальной высоты (стр. 33)
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp), // Минимальная высота
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        ),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.padding(vertical = 8.dp)) {
                                if (currentText.isEmpty()) {
                                    Text(
                                        text = "Введите текст заметки",
                                        fontSize = 16.sp,
                                        color = Color.Gray
                                    )
                                }
                                innerTextField()
                            }
                        },
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                } // Конец Column
            } // Конец else (loading)
        } // Конец Scaffold
    }
}

// Вспомогательный объект для кодирования URL
object UriEscape {
    fun encode(s: String): String {
        return try {
            java.net.URLEncoder.encode(s, "UTF-8")
                .replace("+", "%20") // Пробелы кодируются как %20, а не +
        } catch (e: java.io.UnsupportedEncodingException) {
            s // Fallback
        }
    }
}


// Preview для NoteActivityScreen
@Preview(showBackground = true)
@Composable
fun PreviewNoteActivityScreen() {
    val navController = rememberNavController()
    // Создаем экземпляр для новой заметки (id = "-1")
    NoteActivity(navController, "-1").NoteActivityScreen()
}