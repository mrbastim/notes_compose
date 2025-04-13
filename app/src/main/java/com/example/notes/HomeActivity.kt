package com.example.notesappcompose

import android.annotation.SuppressLint // Для @SuppressLint("NewApi") если нужно
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator // Индикатор загрузки
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import org.threeten.bp.ZonedDateTime // Для сортировки по дате
import java.util.concurrent.CompletableFuture

class HomeActivity(private val navController: NavHostController) {

    // Объекты для работы с API и JSON (стр. 26)
    private val json = jacksonObjectMapper()
    private val api = APISender()

    // Состояние для хранения списка заметок (стр. 26)
    private val notes = mutableStateOf<List<NoteViewModel>>(emptyList())
    // Состояние для отслеживания статуса загрузки (стр. 26)
    private val isNotesUploaded = mutableStateOf(false)

    init { // Выполняется при создании экземпляра HomeActivity
        loadNotes()
    }

    // Функция загрузки заметок
    @SuppressLint("NewApi") // CompletableFuture требует API 24+, используем SuppressLint или core-ktx
    private fun loadNotes() {
        // Запускаем сетевой запрос в фоновом потоке (стр. 26)
        // GlobalScope не рекомендуется в реальных приложениях, лучше использовать viewModelScope или lifecycleScope
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // ВАЖНО: Убедитесь, что ваш бэкенд по адресу "/GetAllNotes" возвращает СПИСОК всех заметок в JSON
                val notesFuture: CompletableFuture<Response> = api.get("/GetAllNotes")
                val notesResponse = notesFuture.get() // Ожидаем результат

                if (notesResponse.isSuccessful) {
                    val body = notesResponse.body?.string() // Получаем тело ответа как строку
                    if (body != null) {
                        // Десериализуем JSON строку в список объектов NoteViewModel (стр. 26)
                        val notesList: List<NoteViewModel> = json.readValue(body)

                        // Сортировка заметок по дате обновления (новейшие сначала) (стр. 26)
                        val sortedNotes = notesList.sortedByDescending {
                            // Пытаемся распарсить дату, нужна обработка ошибок формата
                            try { ZonedDateTime.parse(it.UpdatedAt) } catch (e: Exception) { null }
                        }

                        // Переключаемся обратно в главный поток для обновления UI (стр. 26)
                        withContext(Dispatchers.Main) {
                            notes.value = sortedNotes // Обновляем состояние списка
                            isNotesUploaded.value = true // Указываем, что загрузка завершена
                        }
                    } else {
                        // Обработка случая пустого тела ответа
                        withContext(Dispatchers.Main) { isNotesUploaded.value = true } // Завершаем загрузку
                    }
                } else {
                    // Обработка ошибки HTTP
                    withContext(Dispatchers.Main) { isNotesUploaded.value = true } // Завершаем загрузку
                }
                // Закрываем тело ответа, чтобы освободить ресурсы (стр. 26)
                notesResponse.close()

            } catch (e: Exception) {
                // Обработка исключений (сетевая ошибка, ошибка парсинга JSON и т.д.)
                e.printStackTrace() // Логируем ошибку
                withContext(Dispatchers.Main) {
                    isNotesUploaded.value = true // Завершаем загрузку, даже если с ошибкой
                }
            }
        }
    }

    // Composable функция для отображения UI главного экрана (стр. 27-29)
    @Composable
    fun HomeActivityScreen() {
        // Получаем значения из состояния с помощью 'by remember' (стр. 27)
        val notesList by remember { notes }
        val uploaded by remember { isNotesUploaded }

        Box(modifier = Modifier.fillMaxSize()) { // Контейнер на весь экран

            Column( // Основная колонка для контента
                modifier = Modifier
                    .fillMaxSize()
                    // Отступы как в примере (стр. 28)
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp)
            ) {
                // Заголовок "Заметки" (стр. 28)
                Text(
                    text = Destinations.HomeScreen.title ?: "Заметки",
                    fontSize = 25.sp,
                    color = Color.Black, // Цвет как в примере
                    fontWeight = FontWeight.Bold // Сделаем жирным
                )

                Spacer(modifier = Modifier.height(20.dp)) // Отступ после заголовка (стр. 28)

                // Отображение списка заметок или сообщения о загрузке (стр. 28)
                if (uploaded) { // Если загрузка завершена
                    if (notesList.isNotEmpty()) {
                        // Используем LazyColumn для эффективного отображения списка (стр. 28)
                        LazyColumn(
                            modifier = Modifier.weight(1f), // Занимает оставшееся место
                            state = rememberLazyListState() // Состояние для прокрутки
                        ) {
                            items(notesList) { note -> // Перебираем заметки (стр. 28)
                                NoteItem(note = note, navController = navController) // Отображаем элемент заметки
                                Spacer(modifier = Modifier.height(16.dp)) // Отступ между заметками (стр. 29)
                            }
                        }
                    } else {
                        // Если список пуст после загрузки
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Список заметок пуст")
                        }
                    }
                } else { // Если идет загрузка
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Загрузка заметок...", Modifier.padding(top = 15.dp))
                        CircularProgressIndicator(Modifier.padding(bottom = 60.dp)) // показываем индикатор загрузки
                    }
                }
            } // Конец Column

            // Кнопка добавления новой заметки "+" (стр. 29)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd) // Расположение внизу справа
                    .padding(20.dp) // Отступы
                    .size(50.dp) // Размер
                    .background(Color.Black, shape = RoundedCornerShape(25.dp)) // Фон и скругление
                    .clickable { // Обработка нажатия
                        // Переход на экран заметки с ID = -1 (означает создание новой)
                        navController.navigate(Destinations.NoteScreen.route + "/-1")
                    },
                contentAlignment = Alignment.Center // Центрирование содержимого (+)
            ) {
                Text(
                    text = "+",
                    color = Color.White,
                    fontSize = 25.sp
                )
            } // Конец Box "+"

        } // Конец Box (основной)
    }
}

// Composable функция для отображения одного элемента списка (стр. 29-30)
@Composable
fun NoteItem(note: NoteViewModel, navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxWidth() // На всю ширину
            .height(100.dp) // Фиксированная высота как в примере
            .background(Color(0xFFF5F4F2), shape = RoundedCornerShape(8.dp)) // Фон и скругление
            .padding(vertical = 7.dp, horizontal = 10.dp) // Внутренние отступы
            .clickable { // Обработка нажатия на элемент
                // Переход на экран заметки с ее реальным ID
                navController.navigate(Destinations.NoteScreen.route + "/${note.Id}")
            }
    ) {
        Column( // Колонка для текста внутри элемента
            modifier = Modifier.fillMaxSize() // Занимает все место внутри Box
        ) {
            // Отображение заголовка (стр. 30)
            Text(
                text = if (note.Title.isNotEmpty()) note.Title else "Заголовок отсутствует",
                color = if (note.Title.isNotEmpty()) Color(0xFF222222) else Color(0xFF9C9C9C),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2, // Максимум 2 строки для заголовка
                overflow = TextOverflow.Ellipsis // Многоточие если не влезает
            )
            Spacer(modifier = Modifier.height(4.dp)) // Небольшой отступ

            // Отображение текста заметки (стр. 30)
            Text(
                text = if (note.Text.isNotEmpty()) note.Text else "Текст отсутствует",
                color = Color(0xFF444444), // Чуть светлее заголовка
                fontSize = 11.sp, // Меньший размер
                maxLines = 3, // Максимум 3 строки для текста (в примере 5, но 100dp мало)
                overflow = TextOverflow.Ellipsis // Многоточие
            )
        }
    }
}

// Preview функция для HomeActivityScreen (стр. 30)
@Preview(showBackground = true)
@Composable
fun PreviewHomeActivityScreen() {
    // Создаем фейковый NavController для превью
    val navController = rememberNavController()
    // Создаем экземпляр HomeActivity и вызываем его экран
    HomeActivity(navController).HomeActivityScreen()
    // Примечание: В превью не будет реальной загрузки данных.
    // Чтобы увидеть список, можно модифицировать HomeActivity
    // для принятия начального списка в конструкторе или использовать фейковые данные в превью.
}

// Preview функция для NoteItem (стр. 31)
@Preview(showBackground = true, widthDp = 300)
@Composable
fun PreviewNoteItem() {
    val navController = rememberNavController()
    val sampleNote = NoteViewModel(
        Id = "123",
        Title = "Пример заголовка",
        Text = "Это пример текста заметки, который может быть достаточно длинным, чтобы проверить перенос строк и многоточие.",
        CreatedAt = "2023-01-01T10:00:00Z",
        UpdatedAt = "2023-01-01T11:00:00Z"
    )
    NoteItem(note = sampleNote, navController = navController)
}