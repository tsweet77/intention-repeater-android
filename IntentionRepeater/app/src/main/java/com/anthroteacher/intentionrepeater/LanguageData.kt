package com.anthroteacher.intentionrepeater

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.intellij.lang.annotations.Language

data class Language(val code: String, val displayName: String)

// Initialize the list of languages
val languages = listOf(
    Language(code = "en", displayName = "English"),
    Language(code = "ar", displayName = "Arabic (العربية)"),
    Language(code = "bn", displayName = "Bengali (বাংলা)"),
    Language(code = "zh", displayName = "Chinese (Simplified) (中文)"),
    Language(code = "da", displayName = "Danish (Dansk)"),
    Language(code = "nl", displayName = "Dutch (Nederlands)"),
    Language(code = "fi", displayName = "Finnish (Suomi)"),
    Language(code = "fil", displayName = "Filipino (Filipino)"),
    Language(code = "fr", displayName = "French (Français)"),
    Language(code = "de", displayName = "German (Deutsch)"),
    Language(code = "gu", displayName = "Gujarati (ગુજરાતી)"),
    Language(code = "he", displayName = "Hebrew (עברית)"),
    Language(code = "hi", displayName = "Hindi (हिंदी)"),
    Language(code = "id", displayName = "Indonesian (Bahasa Indonesia)"),
    Language(code = "it", displayName = "Italian (Italiano)"),
    Language(code = "ja", displayName = "Japanese (日本語)"),
    Language(code = "kn", displayName = "Kannada (ಕನ್ನಡ)"),
    Language(code = "ko", displayName = "Korean (한국어)"),
    Language(code = "ms", displayName = "Malay (Bahasa Melayu)"),
    Language(code = "ml", displayName = "Malayalam (മലയാളം)"),
    Language(code = "mr", displayName = "Marathi (मराठी)"),
    Language(code = "nb", displayName = "Norwegian (Norsk Bokmål)"),
    Language(code = "pl", displayName = "Polish (Polski)"),
    Language(code = "pt-BR", displayName = "Portuguese (Brazil) (Português)"),
    Language(code = "pa", displayName = "Punjabi (ਪੰਜਾਬੀ)"),
    Language(code = "ru", displayName = "Russian (Русский)"),
    Language(code = "sa", displayName = "Sanskrit (संस्कृतम्)"),
    Language(code = "es", displayName = "Spanish (Español)"),
    Language(code = "sw", displayName = "Swahili (Kiswahili)"),
    Language(code = "sv", displayName = "Swedish (Svenska)"),
    Language(code = "ta", displayName = "Tamil (தமிழ்)"),
    Language(code = "te", displayName = "Telugu (తెలుగు)"),
    Language(code = "th", displayName = "Thai (ไทย)"),
    Language(code = "tr", displayName = "Turkish (Türkçe)"),
    Language(code = "uk", displayName = "Ukrainian (Українська)"),
    Language(code = "vi", displayName = "Vietnamese (Tiếng Việt)")
)