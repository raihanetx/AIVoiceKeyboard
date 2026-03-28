'use client'

import { useState, useEffect, useCallback } from 'react'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Switch } from '@/components/ui/switch'
import { Input } from '@/components/ui/input'
import { Separator } from '@/components/ui/separator'
import {
  Mic,
  Globe,
  Clipboard,
  Smile,
  CreditCard,
  Settings,
  ArrowLeftRight,
  Copy,
  Trash2,
  Pin,
  Search,
  X,
  Check,
  Eye,
  EyeOff,
  Plus,
  ChevronDown,
  Volume2,
  Keyboard,
  Moon,
  Sun,
  Vibrate,
  Sparkles,
  Undo2
} from 'lucide-react'

// English keyboard layout
const englishKeys = [
  ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0'],
  ['q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'],
  ['a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l'],
  ['z', 'x', 'c', 'v', 'b', 'n', 'm']
]

// Bangla keyboard layout (Probhat)
const banglaKeys = [
  ['১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯', '০'],
  ['অ', 'আ', 'ই', 'ঈ', 'উ', 'ঊ', 'ঋ', 'এ', 'ঐ', 'ও'],
  ['ক', 'খ', 'গ', 'ঘ', 'ঙ', 'চ', 'ছ', 'জ', 'ঝ', 'ঞ'],
  ['ট', 'ঠ', 'ড', 'ঢ', 'ণ', 'ত', 'থ', 'দ', 'ধ', 'ন'],
  ['প', 'ফ', 'ব', 'ভ', 'ম', 'য', 'র', 'ল', 'শ', 'ষ']
]

// English word predictions
const englishPredictions: Record<string, string[]> = {
  'h': ['Hello', 'Hi', 'Hey', 'How'],
  'he': ['Hello', 'Help', 'Hey', 'Here'],
  'hel': ['Hello', 'Help', 'Hello there', 'Help me'],
  'i': ['I', 'I am', 'I will', 'I can'],
  'th': ['The', 'Thank', 'This', 'That'],
  'tha': ['Thank', 'Thanks', 'Thank you', 'That'],
  'ho': ['How', 'Home', 'Hope', 'House'],
  'how': ['How are you', 'How is', 'How to', 'How about'],
  'go': ['Good', 'Going', 'Gone', 'Got'],
  'goo': ['Good', 'Good morning', 'Good night', 'Goodbye'],
  'a': ['a', 'am', 'are', 'and'],
  'w': ['What', 'Where', 'When', 'Why'],
  'wh': ['What', 'Where', 'When', 'Why'],
  'wha': ['What', 'What is', 'What are', 'What about'],
}

// Bangla word predictions
const banglaPredictions: Record<string, string[]> = {
  'আ': ['আমি', 'আপনি', 'আছি', 'আজ'],
  'আম': ['আমি', 'আমরা', 'আমার', 'আমাকে'],
  'ক': ['কর', 'করি', 'করে', 'কথা'],
  'কর': ['কর', 'করি', 'করে', 'করছি'],
  'ভ': ['ভালো', 'ভাই', 'ভাষা', 'ভাবছি'],
  'ভা': ['ভালো', 'ভাই', 'ভাষা', 'ভাবছি'],
  'শ': ['শুভ', 'শোন', 'শিখ', 'শান্ত'],
  'শু': ['শুভ', 'শুনুন', 'শুভকামনা', 'শুভ সকাল'],
  'ধ': ['ধন্যবাদ', 'ধর', 'ধরা', 'ধারণা'],
  'ধন': ['ধন্যবাদ', 'ধনী', 'ধনু', 'ধন্য'],
}

// Emoji categories
const emojiCategories = {
  recent: ['😀', '😎', '🔥', '💯', '✨', '🎉', '👍', '👏', '🙏', '😂'],
  smileys: ['😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗', '☺️', '😚'],
  people: [' 👨', '👩', '👶', '👴', '👵', '🙋', '🙋‍♀️', '🙋‍♂️', '🤦', '🤦‍♀️', '🤦‍♂️', '🤷', '🤷‍♀️', '🤷‍♂️', '💆', '💆‍♀️', '💆‍♂️', '💇', '💇‍♀️', '💇‍♂️'],
  animals: ['🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐨', '🐯', '🦁', '🐮', '🐷', '🐸', '🐵', '🐔', '🐧', '🐦', '🐤', '🦆'],
  food: ['🍎', '🍐', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓', '🍈', '🍒', '🍑', '🥭', '🍍', '🥥', '🥝', '🍅', '🍆', '🥑', '🥦', '🥬'],
  symbols: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️']
}

// Clipboard items
interface ClipboardItem {
  id: number
  text: string
  pinned: boolean
  timestamp: string
}

// Credit card
interface Card {
  id: number
  type: 'visa' | 'mastercard'
  lastFour: string
  expiry: string
  holder: string
}

// Translation history
interface TranslationItem {
  id: number
  sourceText: string
  translatedText: string
  sourceLang: 'en' | 'bn'
}

export default function KeyboardDemo() {
  // State
  const [isEnglish, setIsEnglish] = useState(true)
  const [inputText, setInputText] = useState('')
  const [predictions, setPredictions] = useState<string[]>([])
  const [showVoicePanel, setShowVoicePanel] = useState(false)
  const [showTranslationPanel, setShowTranslationPanel] = useState(false)
  const [showClipboardPanel, setShowClipboardPanel] = useState(false)
  const [showEmojiPanel, setShowEmojiPanel] = useState(false)
  const [showAutofillPanel, setShowAutofillPanel] = useState(false)
  const [showSettingsPanel, setShowSettingsPanel] = useState(false)
  const [isRecording, setIsRecording] = useState(false)
  const [voiceText, setVoiceText] = useState('')
  const [waveformBars, setWaveformBars] = useState<number[]>(Array(20).fill(20))
  
  // Translation state
  const [translateFrom, setTranslateFrom] = useState<'en' | 'bn'>('en')
  const [translateTo, setTranslateTo] = useState<'en' | 'bn'>('bn')
  const [sourceText, setSourceText] = useState('')
  const [translatedText, setTranslatedText] = useState('')
  const [isTranslating, setIsTranslating] = useState(false)
  const [translationHistory, setTranslationHistory] = useState<TranslationItem[]>([])
  
  // Clipboard state
  const [clipboardItems, setClipboardItems] = useState<ClipboardItem[]>([
    { id: 1, text: 'example@gmail.com', pinned: true, timestamp: '2 days ago' },
    { id: 2, text: 'Hello, how are you?', pinned: false, timestamp: '5 min ago' },
    { id: 3, text: 'আমি বাংলায় কথা বলি', pinned: false, timestamp: '10 min ago' },
    { id: 4, text: '+880 1XXX-XXXXXX', pinned: true, timestamp: '1 week ago' },
  ])
  
  // Autofill state
  const [cards, setCards] = useState<Card[]>([
    { id: 1, type: 'visa', lastFour: '4242', expiry: '12/25', holder: 'JOHN DOE' },
    { id: 2, type: 'mastercard', lastFour: '5555', expiry: '08/24', holder: 'JOHN DOE' },
  ])
  const [personalInfo] = useState({
    name: 'John Doe',
    email: 'example@gmail.com',
    phone: '+880 1XXX-XXXXXX',
    address: 'Dhaka, Bangladesh'
  })
  const [showCardNumber, setShowCardNumber] = useState<number | null>(null)
  
  // Settings state
  const [settings, setSettings] = useState({
    darkMode: true,
    vibration: true,
    sound: false,
    predictions: true,
    autocorrect: true,
    numberRow: true,
    swipeTyping: true,
    clipboardHistory: true
  })
  
  // Emoji state
  const [emojiCategory, setEmojiCategory] = useState<'recent' | 'smileys' | 'people' | 'animals' | 'food' | 'symbols'>('recent')
  const [recentEmojis, setRecentEmojis] = useState<string[]>(emojiCategories.recent)

  // Word prediction logic
  const updatePredictions = useCallback((text: string) => {
    const words = text.toLowerCase().split(' ')
    const lastWord = words[words.length - 1]
    const predictionMap = isEnglish ? englishPredictions : banglaPredictions
    
    if (lastWord && predictionMap[lastWord]) {
      setPredictions(predictionMap[lastWord])
    } else if (lastWord.length > 0) {
      // Default predictions based on language
      const defaults = isEnglish 
        ? ['Hello', 'Hi', 'How are you', 'Thank you']
        : ['আমি', 'আপনি', 'কেমন', 'ধন্যবাদ']
      setPredictions(defaults.slice(0, 4))
    } else {
      setPredictions(isEnglish 
        ? ['I', 'The', 'Hello', 'How']
        : ['আমি', 'আপনি', 'এটা', 'কেমন']
      )
    }
  }, [isEnglish])

  // Handle key press
  const handleKeyPress = (key: string) => {
    setInputText(prev => prev + key)
  }

  // Handle backspace
  const handleBackspace = () => {
    setInputText(prev => prev.slice(0, -1))
  }

  // Handle space
  const handleSpace = () => {
    setInputText(prev => prev + ' ')
  }

  // Handle prediction click
  const handlePredictionClick = (prediction: string) => {
    const words = inputText.split(' ')
    words[words.length - 1] = prediction
    setInputText(words.join(' ') + ' ')
  }

  // Handle enter
  const handleEnter = () => {
    setInputText(prev => prev + '\n')
  }

  // Toggle language
  const toggleLanguage = () => {
    setIsEnglish(prev => !prev)
    setPredictions(isEnglish 
      ? ['আমি', 'আপনি', 'এটা', 'কেমন']
      : ['I', 'The', 'Hello', 'How']
    )
  }

  // Simulate voice recording
  useEffect(() => {
    if (isRecording) {
      const interval = setInterval(() => {
        setWaveformBars(prev => prev.map(() => Math.random() * 80 + 20))
      }, 100)
      return () => clearInterval(interval)
    }
  }, [isRecording])

  // Simulate voice transcription
  useEffect(() => {
    if (isRecording) {
      const texts = isEnglish 
        ? ['Hello', 'Hello, how', 'Hello, how are', 'Hello, how are you', 'Hello, how are you doing']
        : ['হ্যালো', 'হ্যালো, আপনি', 'হ্যালো, আপনি কেমন', 'হ্যালো, আপনি কেমন আছেন']
      
      let index = 0
      const interval = setInterval(() => {
        if (index < texts.length) {
          setVoiceText(texts[index])
          index++
        }
      }, 800)
      
      return () => clearInterval(interval)
    }
  }, [isRecording, isEnglish])

  // Simulate translation
  const handleTranslate = async () => {
    if (!sourceText.trim()) return
    
    setIsTranslating(true)
    
    // Simulate API call
    setTimeout(() => {
      // Simple mock translation
      const translations: Record<string, string> = {
        'Hello': 'হ্যালো',
        'Hello, how are you?': 'হ্যালো, আপনি কেমন আছেন?',
        'Thank you': 'ধন্যবাদ',
        'Good morning': 'সুপ্রভাত',
        'Good night': 'শুভ রাত্রি',
        'How are you?': 'আপনি কেমন আছেন?',
        'I am fine': 'আমি ভালো আছি',
        'What is your name?': 'আপনার নাম কী?',
        'হ্যালো': 'Hello',
        'ধন্যবাদ': 'Thank you',
        'সুপ্রভাত': 'Good morning',
        'আপনি কেমন আছেন?': 'How are you?',
      }
      
      setTranslatedText(translations[sourceText] || (translateFrom === 'en' 
        ? `বাংলা অনুবাদ: ${sourceText}`
        : `English translation: ${sourceText}`
      ))
      
      setTranslationHistory(prev => [{
        id: Date.now(),
        sourceText,
        translatedText: translations[sourceText] || sourceText,
        sourceLang: translateFrom
      }, ...prev].slice(0, 10))
      
      setIsTranslating(false)
    }, 1000)
  }

  // Swap languages
  const swapLanguages = () => {
    setTranslateFrom(translateTo)
    setTranslateTo(translateFrom)
    setSourceText(translatedText)
    setTranslatedText(sourceText)
  }

  // Insert voice text
  const insertVoiceText = () => {
    setInputText(prev => prev + voiceText)
    setShowVoicePanel(false)
    setIsRecording(false)
    setVoiceText('')
  }

  // Insert translated text
  const insertTranslatedText = () => {
    setInputText(prev => prev + translatedText)
    setShowTranslationPanel(false)
  }

  // Insert emoji
  const insertEmoji = (emoji: string) => {
    setInputText(prev => prev + emoji)
    setRecentEmojis(prev => [emoji, ...prev.filter(e => e !== emoji)].slice(0, 10))
    setShowEmojiPanel(false)
  }

  // Insert clipboard item
  const insertClipboardItem = (text: string) => {
    setInputText(prev => prev + text)
    setShowClipboardPanel(false)
  }

  // Toggle pin clipboard
  const togglePinClipboard = (id: number) => {
    setClipboardItems(prev => prev.map(item => 
      item.id === id ? { ...item, pinned: !item.pinned } : item
    ))
  }

  // Delete clipboard item
  const deleteClipboardItem = (id: number) => {
    setClipboardItems(prev => prev.filter(item => item.id !== id))
  }

  // Insert card info
  const insertCardInfo = (card: Card) => {
    setInputText(prev => prev + `Card ending in ${card.lastFour}`)
    setShowAutofillPanel(false)
  }

  // Insert personal info
  const insertPersonalInfo = (field: keyof typeof personalInfo) => {
    setInputText(prev => prev + personalInfo[field])
    setShowAutofillPanel(false)
  }

  // Update predictions when input changes
  useEffect(() => {
    updatePredictions(inputText)
  }, [inputText, updatePredictions])

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white p-4 md:p-8">
      {/* Header */}
      <div className="text-center mb-6">
        <h1 className="text-3xl md:text-4xl font-bold bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
          AI Voice Keyboard Demo
        </h1>
        <p className="text-slate-400 mt-2">English & বাংলা | Powered by Gemini AI & GLM-4.7-Flash</p>
      </div>

      {/* Main Demo Area */}
      <div className="max-w-4xl mx-auto">
        {/* Phone Mockup */}
        <div className="bg-slate-950 rounded-[3rem] p-3 shadow-2xl border border-slate-700">
          <div className="bg-slate-900 rounded-[2.5rem] overflow-hidden">
            {/* Status Bar */}
            <div className="flex justify-between items-center px-6 py-2 text-xs text-slate-400">
              <span>9:41</span>
              <div className="flex gap-1">
                <span>📶</span>
                <span>WiFi</span>
                <span>🔋</span>
              </div>
            </div>

            {/* App Content */}
            <div className="px-4 pb-4">
              {/* Demo Text Field */}
              <div className="bg-slate-800/50 rounded-2xl p-4 mb-3 min-h-[120px] border border-slate-700">
                <p className="text-slate-300 text-sm mb-2">Text Input Area:</p>
                <p className="text-lg leading-relaxed break-words">
                  {inputText || <span className="text-slate-500 italic">Start typing...</span>}
                </p>
              </div>

              {/* Word Predictions */}
              {settings.predictions && (
                <div className="flex gap-2 mb-3 overflow-x-auto pb-2">
                  {predictions.map((pred, idx) => (
                    <button
                      key={idx}
                      onClick={() => handlePredictionClick(pred)}
                      className="flex-shrink-0 px-4 py-2 bg-slate-700/50 hover:bg-slate-600/50 rounded-full text-sm transition-all duration-200 border border-slate-600 hover:border-slate-500"
                    >
                      {pred}
                    </button>
                  ))}
                </div>
              )}

              {/* Keyboard */}
              <div className="bg-slate-800/80 rounded-2xl p-2 backdrop-blur-sm">
                {/* Toolbar */}
                <div className="flex justify-between items-center px-2 py-1 mb-2 border-b border-slate-700/50">
                  <div className="flex gap-1">
                    <button 
                      onClick={() => setShowClipboardPanel(true)}
                      className={`p-2 rounded-lg transition-colors ${showClipboardPanel ? 'bg-blue-500/20 text-blue-400' : 'hover:bg-slate-700/50'}`}
                    >
                      <Clipboard className="w-5 h-5" />
                    </button>
                    <button 
                      onClick={() => setShowEmojiPanel(true)}
                      className={`p-2 rounded-lg transition-colors ${showEmojiPanel ? 'bg-blue-500/20 text-blue-400' : 'hover:bg-slate-700/50'}`}
                    >
                      <Smile className="w-5 h-5" />
                    </button>
                    <button 
                      onClick={() => setShowTranslationPanel(true)}
                      className={`p-2 rounded-lg transition-colors ${showTranslationPanel ? 'bg-blue-500/20 text-blue-400' : 'hover:bg-slate-700/50'}`}
                    >
                      <Globe className="w-5 h-5" />
                    </button>
                    <button 
                      onClick={() => setShowAutofillPanel(true)}
                      className={`p-2 rounded-lg transition-colors ${showAutofillPanel ? 'bg-blue-500/20 text-blue-400' : 'hover:bg-slate-700/50'}`}
                    >
                      <CreditCard className="w-5 h-5" />
                    </button>
                  </div>
                  
                  <button 
                    onClick={toggleLanguage}
                    className="px-3 py-1 bg-blue-500/20 hover:bg-blue-500/30 rounded-lg text-sm font-medium transition-colors"
                  >
                    {isEnglish ? 'EN' : 'বাংলা'}
                  </button>
                  
                  <button 
                    onClick={() => setShowSettingsPanel(true)}
                    className={`p-2 rounded-lg transition-colors ${showSettingsPanel ? 'bg-blue-500/20 text-blue-400' : 'hover:bg-slate-700/50'}`}
                  >
                    <Settings className="w-5 h-5" />
                  </button>
                </div>

                {/* Number Row */}
                {settings.numberRow && (
                  <div className="flex gap-1 mb-1 justify-center">
                    {(isEnglish ? englishKeys[0] : banglaKeys[0]).map((key) => (
                      <button
                        key={key}
                        onClick={() => handleKeyPress(key)}
                        className="w-8 h-10 bg-slate-700/50 hover:bg-slate-600/50 rounded-lg text-sm font-medium transition-colors"
                      >
                        {key}
                      </button>
                    ))}
                  </div>
                )}

                {/* Letter Keys */}
                {(isEnglish ? englishKeys.slice(1) : banglaKeys.slice(1)).map((row, rowIdx) => (
                  <div key={rowIdx} className="flex gap-1 mb-1 justify-center">
                    {rowIdx === 2 && isEnglish && (
                      <button className="w-12 h-12 bg-slate-700/30 rounded-lg text-xs">
                        ⇧
                      </button>
                    )}
                    {row.map((key) => (
                      <button
                        key={key}
                        onClick={() => handleKeyPress(key)}
                        className="w-9 h-12 bg-slate-700/50 hover:bg-slate-600/50 rounded-lg text-lg font-medium transition-colors active:scale-95"
                      >
                        {key}
                      </button>
                    ))}
                    {rowIdx === 2 && isEnglish && (
                      <button 
                        onClick={handleBackspace}
                        className="w-12 h-12 bg-slate-700/30 hover:bg-red-500/20 rounded-lg text-xs transition-colors"
                      >
                        ⌫
                      </button>
                    )}
                  </div>
                ))}

                {/* Bottom Row */}
                <div className="flex gap-1 mt-1 justify-center items-center">
                  <button className="w-14 h-12 bg-slate-700/30 rounded-lg text-xs font-medium">
                    ?123
                  </button>
                  <button 
                    onClick={() => setShowVoicePanel(true)}
                    className="w-12 h-12 bg-red-500/20 hover:bg-red-500/30 rounded-full flex items-center justify-center transition-colors"
                  >
                    <Mic className="w-5 h-5 text-red-400" />
                  </button>
                  <button 
                    onClick={handleSpace}
                    className="w-32 h-12 bg-slate-700/50 hover:bg-slate-600/50 rounded-lg text-sm font-medium transition-colors"
                  >
                    {isEnglish ? 'Space' : 'স্পেস'}
                  </button>
                  <button 
                    onClick={() => setShowTranslationPanel(true)}
                    className="w-10 h-12 bg-purple-500/20 hover:bg-purple-500/30 rounded-lg flex items-center justify-center transition-colors"
                  >
                    <Globe className="w-4 h-4 text-purple-400" />
                  </button>
                  <button 
                    onClick={handleEnter}
                    className="w-14 h-12 bg-blue-500/20 hover:bg-blue-500/30 rounded-lg text-sm font-medium transition-colors"
                  >
                    ↵
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Feature Buttons */}
        <div className="flex flex-wrap justify-center gap-3 mt-6">
          <Button 
            onClick={() => setShowVoicePanel(true)}
            className="bg-gradient-to-r from-red-500 to-pink-500 hover:from-red-600 hover:to-pink-600"
          >
            <Mic className="w-4 h-4 mr-2" />
            Voice Input
          </Button>
          <Button 
            onClick={() => setShowTranslationPanel(true)}
            className="bg-gradient-to-r from-purple-500 to-indigo-500 hover:from-purple-600 hover:to-indigo-600"
          >
            <Globe className="w-4 h-4 mr-2" />
            Translate
          </Button>
          <Button 
            onClick={() => setShowClipboardPanel(true)}
            className="bg-gradient-to-r from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600"
          >
            <Clipboard className="w-4 h-4 mr-2" />
            Clipboard
          </Button>
          <Button 
            onClick={() => setShowEmojiPanel(true)}
            className="bg-gradient-to-r from-yellow-500 to-orange-500 hover:from-yellow-600 hover:to-orange-600"
          >
            <Smile className="w-4 h-4 mr-2" />
            Emoji
          </Button>
          <Button 
            onClick={() => setShowAutofillPanel(true)}
            className="bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
          >
            <CreditCard className="w-4 h-4 mr-2" />
            Autofill
          </Button>
          <Button 
            onClick={() => setShowSettingsPanel(true)}
            className="bg-gradient-to-r from-slate-500 to-slate-600 hover:from-slate-600 hover:to-slate-700"
          >
            <Settings className="w-4 h-4 mr-2" />
            Settings
          </Button>
        </div>
      </div>

      {/* Voice Panel */}
      {showVoicePanel && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 rounded-3xl p-6 w-full max-w-md border border-slate-700">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold flex items-center gap-2">
                <Mic className="w-6 h-6 text-red-400" />
                Voice Input
              </h2>
              <Button variant="ghost" size="icon" onClick={() => {
                setShowVoicePanel(false)
                setIsRecording(false)
                setVoiceText('')
              }}>
                <X className="w-5 h-5" />
              </Button>
            </div>

            {/* Language Selection */}
            <div className="flex gap-2 mb-6">
              <button
                onClick={() => setIsEnglish(true)}
                className={`flex-1 py-2 rounded-xl font-medium transition-colors ${isEnglish ? 'bg-blue-500/20 text-blue-400 border border-blue-500/50' : 'bg-slate-800 text-slate-400'}`}
              >
                English
              </button>
              <button
                onClick={() => setIsEnglish(false)}
                className={`flex-1 py-2 rounded-xl font-medium transition-colors ${!isEnglish ? 'bg-blue-500/20 text-blue-400 border border-blue-500/50' : 'bg-slate-800 text-slate-400'}`}
              >
                বাংলা
              </button>
            </div>

            {/* Waveform */}
            <div className="flex items-center justify-center gap-1 h-20 mb-6">
              {waveformBars.map((height, idx) => (
                <div
                  key={idx}
                  className={`w-2 rounded-full transition-all duration-100 ${isRecording ? 'bg-red-500' : 'bg-slate-600'}`}
                  style={{ height: isRecording ? `${height}px` : '20px' }}
                />
              ))}
            </div>

            {/* Transcription */}
            <div className="bg-slate-800/50 rounded-xl p-4 mb-6 min-h-[60px] border border-slate-700">
              {voiceText || <span className="text-slate-500 italic">Tap mic to start recording...</span>}
            </div>

            {/* Controls */}
            <div className="flex justify-center gap-4">
              <Button 
                variant="outline" 
                onClick={() => {
                  setIsRecording(false)
                  setVoiceText('')
                }}
                className="w-32"
              >
                <X className="w-4 h-4 mr-2" />
                Cancel
              </Button>
              <Button 
                onClick={() => setIsRecording(!isRecording)}
                className={`w-20 h-20 rounded-full ${isRecording ? 'bg-red-500 hover:bg-red-600' : 'bg-blue-500 hover:bg-blue-600'}`}
              >
                {isRecording ? <div className="w-6 h-6 bg-white rounded" /> : <Mic className="w-8 h-8" />}
              </Button>
              <Button 
                onClick={insertVoiceText}
                disabled={!voiceText}
                className="w-32"
              >
                <Check className="w-4 h-4 mr-2" />
                Insert
              </Button>
            </div>

            <p className="text-center text-slate-500 text-sm mt-4">
              Powered by Gemini 2.5 Flash Native Audio
            </p>
          </div>
        </div>
      )}

      {/* Translation Panel */}
      {showTranslationPanel && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 rounded-3xl p-6 w-full max-w-lg border border-slate-700">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold flex items-center gap-2">
                <Globe className="w-6 h-6 text-purple-400" />
                Translation
              </h2>
              <Button variant="ghost" size="icon" onClick={() => setShowTranslationPanel(false)}>
                <X className="w-5 h-5" />
              </Button>
            </div>

            {/* Language Selection */}
            <div className="flex items-center justify-center gap-4 mb-6">
              <button
                onClick={() => { setTranslateFrom('en'); setTranslateTo('bn'); }}
                className={`px-4 py-2 rounded-xl font-medium transition-colors ${translateFrom === 'en' ? 'bg-purple-500/20 text-purple-400 border border-purple-500/50' : 'bg-slate-800 text-slate-400'}`}
              >
                English
              </button>
              <button onClick={swapLanguages} className="p-2 rounded-full hover:bg-slate-800 transition-colors">
                <ArrowLeftRight className="w-5 h-5 text-slate-400" />
              </button>
              <button
                onClick={() => { setTranslateFrom('bn'); setTranslateTo('en'); }}
                className={`px-4 py-2 rounded-xl font-medium transition-colors ${translateFrom === 'bn' ? 'bg-purple-500/20 text-purple-400 border border-purple-500/50' : 'bg-slate-800 text-slate-400'}`}
              >
                বাংলা
              </button>
            </div>

            {/* Source Text */}
            <div className="mb-4">
              <label className="text-sm text-slate-400 mb-2 block">
                {translateFrom === 'en' ? 'English' : 'বাংলা'}:
              </label>
              <div className="relative">
                <textarea
                  value={sourceText}
                  onChange={(e) => setSourceText(e.target.value)}
                  className="w-full bg-slate-800/50 rounded-xl p-4 border border-slate-700 focus:border-purple-500/50 focus:outline-none min-h-[80px] resize-none"
                  placeholder={translateFrom === 'en' ? 'Enter English text...' : 'বাংলা লিখুন...'}
                />
                <Button
                  variant="ghost"
                  size="icon"
                  className="absolute right-2 top-2"
                  onClick={() => setSourceText('')}
                >
                  <X className="w-4 h-4" />
                </Button>
              </div>
            </div>

            {/* Translate Button */}
            <Button 
              onClick={handleTranslate}
              disabled={!sourceText.trim() || isTranslating}
              className="w-full mb-4 bg-gradient-to-r from-purple-500 to-indigo-500 hover:from-purple-600 hover:to-indigo-600"
            >
              {isTranslating ? (
                <>
                  <Sparkles className="w-4 h-4 mr-2 animate-spin" />
                  Translating...
                </>
              ) : (
                <>
                  <Globe className="w-4 h-4 mr-2" />
                  Translate Now
                </>
              )}
            </Button>

            {/* Translated Text */}
            {translatedText && (
              <div className="mb-4">
                <label className="text-sm text-slate-400 mb-2 block">
                  {translateTo === 'en' ? 'English' : 'বাংলা'}:
                </label>
                <div className="bg-slate-800/50 rounded-xl p-4 border border-purple-500/30 relative">
                  <p className="text-lg">{translatedText}</p>
                  <div className="absolute right-2 top-2 flex gap-1">
                    <Button variant="ghost" size="icon" onClick={() => navigator.clipboard.writeText(translatedText)}>
                      <Copy className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
                <Button 
                  onClick={insertTranslatedText}
                  className="w-full mt-2"
                  variant="outline"
                >
                  Insert into Text Field
                </Button>
              </div>
            )}

            {/* History */}
            {translationHistory.length > 0 && (
              <div>
                <Separator className="my-4 bg-slate-700" />
                <p className="text-sm text-slate-400 mb-2">History:</p>
                <ScrollArea className="h-24">
                  {translationHistory.map(item => (
                    <button
                      key={item.id}
                      onClick={() => {
                        setSourceText(item.sourceText)
                        setTranslatedText(item.translatedText)
                      }}
                      className="w-full text-left p-2 hover:bg-slate-800/50 rounded-lg mb-1 transition-colors"
                    >
                      <p className="text-sm truncate">{item.sourceText}</p>
                      <p className="text-xs text-slate-500 truncate">{item.translatedText}</p>
                    </button>
                  ))}
                </ScrollArea>
              </div>
            )}

            <p className="text-center text-slate-500 text-sm mt-4">
              Powered by GLM-4.7-Flash via z-ai SDK
            </p>
          </div>
        </div>
      )}

      {/* Clipboard Panel */}
      {showClipboardPanel && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 rounded-3xl p-6 w-full max-w-md border border-slate-700 max-h-[80vh] overflow-hidden flex flex-col">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold flex items-center gap-2">
                <Clipboard className="w-6 h-6 text-green-400" />
                Clipboard Manager
              </h2>
              <div className="flex gap-2">
                <Button variant="ghost" size="icon" onClick={() => setClipboardItems([])}>
                  <Trash2 className="w-5 h-5" />
                </Button>
                <Button variant="ghost" size="icon" onClick={() => setShowClipboardPanel(false)}>
                  <X className="w-5 h-5" />
                </Button>
              </div>
            </div>

            {/* Search */}
            <div className="relative mb-4">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <Input 
                placeholder="Search clipboard..." 
                className="pl-10 bg-slate-800/50 border-slate-700"
              />
            </div>

            {/* Clipboard Items */}
            <ScrollArea className="flex-1">
              {/* Pinned */}
              {clipboardItems.some(item => item.pinned) && (
                <>
                  <p className="text-sm text-slate-400 mb-2 flex items-center gap-1">
                    <Pin className="w-3 h-3" /> Pinned
                  </p>
                  {clipboardItems.filter(item => item.pinned).map(item => (
                    <div
                      key={item.id}
                      className="bg-slate-800/50 rounded-xl p-3 mb-2 border border-slate-700 cursor-pointer hover:bg-slate-700/50 transition-colors"
                      onClick={() => insertClipboardItem(item.text)}
                    >
                      <div className="flex justify-between items-start">
                        <p className="text-sm flex-1 truncate">{item.text}</p>
                        <div className="flex gap-1">
                          <button 
                            onClick={(e) => { e.stopPropagation(); togglePinClipboard(item.id); }}
                            className="p-1 hover:bg-slate-600/50 rounded"
                          >
                            <Pin className="w-4 h-4 text-blue-400" />
                          </button>
                          <button 
                            onClick={(e) => { e.stopPropagation(); navigator.clipboard.writeText(item.text); }}
                            className="p-1 hover:bg-slate-600/50 rounded"
                          >
                            <Copy className="w-4 h-4 text-slate-400" />
                          </button>
                        </div>
                      </div>
                      <p className="text-xs text-slate-500 mt-1">{item.timestamp}</p>
                    </div>
                  ))}
                </>
              )}

              {/* Recent */}
              <p className="text-sm text-slate-400 mb-2 mt-4">Recent</p>
              {clipboardItems.filter(item => !item.pinned).map(item => (
                <div
                  key={item.id}
                  className="bg-slate-800/50 rounded-xl p-3 mb-2 border border-slate-700 cursor-pointer hover:bg-slate-700/50 transition-colors"
                  onClick={() => insertClipboardItem(item.text)}
                >
                  <div className="flex justify-between items-start">
                    <p className="text-sm flex-1 truncate">{item.text}</p>
                    <div className="flex gap-1">
                      <button 
                        onClick={(e) => { e.stopPropagation(); togglePinClipboard(item.id); }}
                        className="p-1 hover:bg-slate-600/50 rounded"
                      >
                        <Pin className="w-4 h-4 text-slate-500" />
                      </button>
                      <button 
                        onClick={(e) => { e.stopPropagation(); deleteClipboardItem(item.id); }}
                        className="p-1 hover:bg-slate-600/50 rounded"
                      >
                        <Trash2 className="w-4 h-4 text-slate-400" />
                      </button>
                    </div>
                  </div>
                  <p className="text-xs text-slate-500 mt-1">{item.timestamp}</p>
                </div>
              ))}
            </ScrollArea>
          </div>
        </div>
      )}

      {/* Emoji Panel */}
      {showEmojiPanel && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 rounded-3xl p-6 w-full max-w-md border border-slate-700">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold flex items-center gap-2">
                <Smile className="w-6 h-6 text-yellow-400" />
                Emoji
              </h2>
              <Button variant="ghost" size="icon" onClick={() => setShowEmojiPanel(false)}>
                <X className="w-5 h-5" />
              </Button>
            </div>

            {/* Search */}
            <div className="relative mb-4">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <Input 
                placeholder="Search emoji..." 
                className="pl-10 bg-slate-800/50 border-slate-700"
              />
            </div>

            {/* Category Tabs */}
            <Tabs defaultValue="recent" className="w-full">
              <TabsList className="grid grid-cols-6 mb-4 bg-slate-800/50">
                <TabsTrigger value="recent">🕐</TabsTrigger>
                <TabsTrigger value="smileys">😀</TabsTrigger>
                <TabsTrigger value="people">👤</TabsTrigger>
                <TabsTrigger value="animals">🐻</TabsTrigger>
                <TabsTrigger value="food">🍕</TabsTrigger>
                <TabsTrigger value="symbols">❤️</TabsTrigger>
              </TabsList>

              {['recent', 'smileys', 'people', 'animals', 'food', 'symbols'].map(category => (
                <TabsContent key={category} value={category}>
                  <div className="grid grid-cols-8 gap-2">
                    {(category === 'recent' ? recentEmojis : emojiCategories[category as keyof typeof emojiCategories]).map((emoji, idx) => (
                      <button
                        key={idx}
                        onClick={() => insertEmoji(emoji)}
                        className="w-10 h-10 text-2xl hover:bg-slate-700/50 rounded-lg transition-colors active:scale-95"
                      >
                        {emoji}
                      </button>
                    ))}
                  </div>
                </TabsContent>
              ))}
            </Tabs>
          </div>
        </div>
      )}

      {/* Autofill Panel */}
      {showAutofillPanel && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 rounded-3xl p-6 w-full max-w-md border border-slate-700 max-h-[80vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold flex items-center gap-2">
                <CreditCard className="w-6 h-6 text-cyan-400" />
                Autofill
              </h2>
              <Button variant="ghost" size="icon" onClick={() => setShowAutofillPanel(false)}>
                <X className="w-5 h-5" />
              </Button>
            </div>

            {/* Cards */}
            <div className="mb-6">
              <p className="text-sm text-slate-400 mb-3 flex items-center gap-2">
                <CreditCard className="w-4 h-4" />
                Cards
              </p>
              {cards.map(card => (
                <div
                  key={card.id}
                  className="bg-gradient-to-r from-slate-800 to-slate-700 rounded-xl p-4 mb-2 border border-slate-600 cursor-pointer hover:border-cyan-500/50 transition-colors"
                  onClick={() => insertCardInfo(card)}
                >
                  <div className="flex justify-between items-center">
                    <div>
                      <p className="font-medium flex items-center gap-2">
                        {card.type === 'visa' ? '💳 Visa' : '💳 Mastercard'}
                        <span className="text-slate-400">•••• {card.lastFour}</span>
                      </p>
                      <p className="text-sm text-slate-500">Expires {card.expiry}</p>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={(e) => {
                        e.stopPropagation()
                        setShowCardNumber(showCardNumber === card.id ? null : card.id)
                      }}
                    >
                      {showCardNumber === card.id ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </Button>
                  </div>
                </div>
              ))}
              <Button variant="outline" className="w-full mt-2">
                <Plus className="w-4 h-4 mr-2" />
                Add New Card
              </Button>
            </div>

            {/* Personal Info */}
            <div className="mb-4">
              <p className="text-sm text-slate-400 mb-3 flex items-center gap-2">
                👤 Personal Information
              </p>
              <div className="bg-slate-800/50 rounded-xl p-4 border border-slate-700 space-y-3">
                <div 
                  className="flex justify-between items-center cursor-pointer hover:bg-slate-700/30 p-2 rounded-lg transition-colors"
                  onClick={() => insertPersonalInfo('name')}
                >
                  <span className="text-sm text-slate-400">Name</span>
                  <span>{personalInfo.name}</span>
                </div>
                <div 
                  className="flex justify-between items-center cursor-pointer hover:bg-slate-700/30 p-2 rounded-lg transition-colors"
                  onClick={() => insertPersonalInfo('email')}
                >
                  <span className="text-sm text-slate-400">Email</span>
                  <span>{personalInfo.email}</span>
                </div>
                <div 
                  className="flex justify-between items-center cursor-pointer hover:bg-slate-700/30 p-2 rounded-lg transition-colors"
                  onClick={() => insertPersonalInfo('phone')}
                >
                  <span className="text-sm text-slate-400">Phone</span>
                  <span>{personalInfo.phone}</span>
                </div>
                <div 
                  className="flex justify-between items-center cursor-pointer hover:bg-slate-700/30 p-2 rounded-lg transition-colors"
                  onClick={() => insertPersonalInfo('address')}
                >
                  <span className="text-sm text-slate-400">Address</span>
                  <span>{personalInfo.address}</span>
                </div>
              </div>
            </div>

            <p className="text-center text-slate-500 text-sm flex items-center justify-center gap-1">
              🔐 All data is encrypted and stored locally
            </p>
          </div>
        </div>
      )}

      {/* Settings Panel */}
      {showSettingsPanel && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 rounded-3xl p-6 w-full max-w-md border border-slate-700 max-h-[80vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold flex items-center gap-2">
                <Settings className="w-6 h-6 text-slate-400" />
                Settings
              </h2>
              <Button variant="ghost" size="icon" onClick={() => setShowSettingsPanel(false)}>
                <X className="w-5 h-5" />
              </Button>
            </div>

            {/* Language Settings */}
            <div className="mb-6">
              <p className="text-sm text-slate-400 mb-3">Language</p>
              <div className="bg-slate-800/50 rounded-xl p-4 border border-slate-700">
                <div className="flex gap-2">
                  <button
                    onClick={() => setIsEnglish(true)}
                    className={`flex-1 py-2 rounded-xl font-medium transition-colors ${isEnglish ? 'bg-blue-500/20 text-blue-400 border border-blue-500/50' : 'bg-slate-700 text-slate-400'}`}
                  >
                    English
                  </button>
                  <button
                    onClick={() => setIsEnglish(false)}
                    className={`flex-1 py-2 rounded-xl font-medium transition-colors ${!isEnglish ? 'bg-blue-500/20 text-blue-400 border border-blue-500/50' : 'bg-slate-700 text-slate-400'}`}
                  >
                    বাংলা
                  </button>
                </div>
              </div>
            </div>

            {/* Typing Settings */}
            <div className="mb-6">
              <p className="text-sm text-slate-400 mb-3">Typing</p>
              <div className="bg-slate-800/50 rounded-xl border border-slate-700 divide-y divide-slate-700">
                <div className="flex justify-between items-center p-4">
                  <div className="flex items-center gap-3">
                    <Sparkles className="w-5 h-5 text-purple-400" />
                    <span>Word Predictions</span>
                  </div>
                  <Switch 
                    checked={settings.predictions}
                    onCheckedChange={(checked) => setSettings(prev => ({ ...prev, predictions: checked }))}
                  />
                </div>
                <div className="flex justify-between items-center p-4">
                  <div className="flex items-center gap-3">
                    <Undo2 className="w-5 h-5 text-green-400" />
                    <span>Autocorrect</span>
                  </div>
                  <Switch 
                    checked={settings.autocorrect}
                    onCheckedChange={(checked) => setSettings(prev => ({ ...prev, autocorrect: checked }))}
                  />
                </div>
                <div className="flex justify-between items-center p-4">
                  <div className="flex items-center gap-3">
                    <Keyboard className="w-5 h-5 text-blue-400" />
                    <span>Number Row</span>
                  </div>
                  <Switch 
                    checked={settings.numberRow}
                    onCheckedChange={(checked) => setSettings(prev => ({ ...prev, numberRow: checked }))}
                  />
                </div>
                <div className="flex justify-between items-center p-4">
                  <div className="flex items-center gap-3">
                    <span className="text-lg">👆</span>
                    <span>Swipe Typing</span>
                  </div>
                  <Switch 
                    checked={settings.swipeTyping}
                    onCheckedChange={(checked) => setSettings(prev => ({ ...prev, swipeTyping: checked }))}
                  />
                </div>
              </div>
            </div>

            {/* Feedback Settings */}
            <div className="mb-6">
              <p className="text-sm text-slate-400 mb-3">Feedback</p>
              <div className="bg-slate-800/50 rounded-xl border border-slate-700 divide-y divide-slate-700">
                <div className="flex justify-between items-center p-4">
                  <div className="flex items-center gap-3">
                    <Vibrate className="w-5 h-5 text-orange-400" />
                    <span>Key Vibration</span>
                  </div>
                  <Switch 
                    checked={settings.vibration}
                    onCheckedChange={(checked) => setSettings(prev => ({ ...prev, vibration: checked }))}
                  />
                </div>
                <div className="flex justify-between items-center p-4">
                  <div className="flex items-center gap-3">
                    <Volume2 className="w-5 h-5 text-cyan-400" />
                    <span>Key Sound</span>
                  </div>
                  <Switch 
                    checked={settings.sound}
                    onCheckedChange={(checked) => setSettings(prev => ({ ...prev, sound: checked }))}
                  />
                </div>
              </div>
            </div>

            {/* Appearance */}
            <div className="mb-4">
              <p className="text-sm text-slate-400 mb-3">Appearance</p>
              <div className="bg-slate-800/50 rounded-xl border border-slate-700 divide-y divide-slate-700">
                <div className="flex justify-between items-center p-4">
                  <div className="flex items-center gap-3">
                    {settings.darkMode ? <Moon className="w-5 h-5 text-indigo-400" /> : <Sun className="w-5 h-5 text-yellow-400" />}
                    <span>Dark Mode</span>
                  </div>
                  <Switch 
                    checked={settings.darkMode}
                    onCheckedChange={(checked) => setSettings(prev => ({ ...prev, darkMode: checked }))}
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Footer */}
      <div className="text-center mt-8 text-slate-500 text-sm">
        <p>AI Voice Keyboard Demo • English & বাংলা</p>
        <p className="mt-1">Voice: Gemini 2.5 Flash • Translation: GLM-4.7-Flash</p>
      </div>
    </div>
  )
}
