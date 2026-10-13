import React, { useState, useEffect } from 'react';
import {
  GraduationCap,
  Clock,
  BookOpen,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Award,
  BarChart3,
  ListChecks,
  ShieldCheck,
  LogOut,
  ChevronRight,
  ChevronLeft,
  Flag,
  Plus,
  Trash2,
  Edit3,
  Eye,
  EyeOff,
  Search,
  User as UserIcon,
  HelpCircle
} from 'lucide-react';

// ============================================================================
// DATA MODELS
// ============================================================================
interface User {
  id: string;
  username: string;
  fullName: string;
  email: string;
  role: 'STUDENT' | 'INSTRUCTOR';
  passwordSalt: string;
  passwordHash: string;
  department: string;
}

interface Question {
  id: string;
  questionText: string;
  options: string[];
  correctAnswerIndex: number;
  concept: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  courseOutcome: string;
  explanation: string;
}

interface Quiz {
  id: string;
  title: string;
  description: string;
  durationMinutes: number;
  passPercentage: number;
  courseOutcome: string;
  questionIds: string[];
  instructions: string[];
}

interface QuestionSnapshot {
  questionId: string;
  questionText: string;
  options: string[];
  correctAnswerIndex: number;
  studentAnswerIndex: number; // -1 for unattempted / unanswered / invalid
  concept: string;
  courseOutcome: string;
  explanation: string;
  status: 'CORRECT' | 'INCORRECT' | 'UNANSWERED' | 'INVALID';
}

interface QuizAttempt {
  id: string;
  quizId: string;
  quizTitle: string;
  studentUsername: string;
  studentFullName: string;
  totalQuestions: number;
  attemptedCount: number;
  unattemptedCount: number;
  invalidCount?: number;
  correctCount: number;
  wrongCount: number;
  score: number;
  maxScore: number;
  percentage: number;
  startTime: string;
  submissionTime: string;
  timeTakenSeconds: number;
  submissionType: 'MANUAL' | 'AUTOMATIC_TIMEOUT';
  snapshots: QuestionSnapshot[];
}

// ============================================================================
// CRYPTOGRAPHIC UTILITIES (PBKDF2-HMAC-SHA256 matching PasswordUtil.java)
// ============================================================================
async function hashPassword(password: string, saltBase64: string): Promise<string> {
  try {
    const enc = new TextEncoder();
    const saltBytes = Uint8Array.from(atob(saltBase64), (c) => c.charCodeAt(0));
    const keyMaterial = await window.crypto.subtle.importKey(
      'raw',
      enc.encode(password),
      { name: 'PBKDF2' },
      false,
      ['deriveBits']
    );
    const key = await window.crypto.subtle.deriveBits(
      {
        name: 'PBKDF2',
        salt: saltBytes,
        iterations: 65536,
        hash: 'SHA-256'
      },
      keyMaterial,
      256
    );
    return btoa(String.fromCharCode(...new Uint8Array(key)));
  } catch (err) {
    // Deterministic fallback if subtle crypto is unavailable in sandbox
    let h = 5381;
    const str = password + ':' + saltBase64;
    for (let i = 0; i < str.length; i++) {
      h = ((h << 5) + h) + str.charCodeAt(i);
      h |= 0;
    }
    return btoa('fallback_' + Math.abs(h));
  }
}

function generateSalt(): string {
  const bytes = new Uint8Array(16);
  window.crypto.getRandomValues(bytes);
  return btoa(String.fromCharCode(...bytes));
}

// ============================================================================
// SEED RECORDS (All demo passwords are "Password123")
// ============================================================================
const SEED_USERS: User[] = [
  {
    id: 'USR-001',
    username: 'instructor_admin',
    fullName: 'Prof. Rajesh Kumar (Faculty Advisor)',
    email: 'rajesh.kumar@alliance.edu.in',
    role: 'INSTRUCTOR',
    passwordSalt: 'FvtVqV35b6/YQUfw+pFUNQ==',
    passwordHash: 'NklMff/7mvjimM4XSPQrB4UA/Ywju1It/dTM11Tue0Q=',
    department: 'Alliance School of Advanced Computing'
  },
  {
    id: 'USR-002',
    username: 'harsha_v',
    fullName: 'Harsha Vardhan E',
    email: 'harsha.v@ced.alliance.edu.in',
    role: 'STUDENT',
    passwordSalt: '6yiG3SPzQE9WYprcQX99mA==',
    passwordHash: 'xA/B7Igrz+xh03tk4JRoPxORY6WfuOoNDqNTxvfLSGA=',
    department: 'Computer Science & Engineering'
  },
  {
    id: 'USR-003',
    username: 'monisha_r',
    fullName: 'Monisha R',
    email: 'monisha.r@ced.alliance.edu.in',
    role: 'STUDENT',
    passwordSalt: 'q3Z7K9EEg5I5Kcv/NbkP5g==',
    passwordHash: '1wuTTgYNX+sUSp9H78js1YWVTKp/huHvI5bz3JpUc/o=',
    department: 'Computer Science & Engineering'
  },
  {
    id: 'USR-004',
    username: 'thanushree_d',
    fullName: 'D Thanushree',
    email: 'thanushree.d@ced.alliance.edu.in',
    role: 'STUDENT',
    passwordSalt: 'TlSW6NvDPZMfgQ8SptxpwQ==',
    passwordHash: 'UvS//uOoJxey7hLE1Tfk+eTOh3621gnEvma1gNkaLJI=',
    department: 'Computer Science & Engineering'
  }
];

const SEED_QUESTIONS: Question[] = [
  {
    id: 'Q-101',
    questionText: 'In Java, what happens if a subclass constructor does not explicitly call super() and the superclass does not have a no-argument constructor?',
    options: [
      'The program compiles and initializes superclass fields to defaults',
      'A compile-time error occurs because super() is inserted implicitly but cannot be resolved',
      'A runtime NoSuchMethodError is thrown during instantiation',
      'The JVM automatically synthesizes an empty constructor in the superclass'
    ],
    correctAnswerIndex: 1,
    concept: 'Inheritance',
    difficulty: 'MEDIUM',
    courseOutcome: 'CO-1',
    explanation: 'If a constructor does not explicitly invoke a superclass constructor, the Java compiler automatically inserts super(). If the superclass lacks an accessible no-arg constructor, compilation fails.'
  },
  {
    id: 'Q-102',
    questionText: 'Which keyword is used by a subclass method to invoke an overridden method in its immediate superclass?',
    options: ['this', 'super', 'parent', 'base'],
    correctAnswerIndex: 1,
    concept: 'Inheritance',
    difficulty: 'EASY',
    courseOutcome: 'CO-1',
    explanation: "The 'super' keyword in Java is used to refer directly to the superclass instance members, allowing access to overridden methods."
  },
  {
    id: 'Q-103',
    questionText: 'What is the mechanism by which a call to an overridden method is resolved at runtime rather than compile time in Java?',
    options: [
      'Static binding',
      'Dynamic Method Dispatch (Runtime Polymorphism)',
      'Method Overloading resolution',
      'Bytecode link preemption'
    ],
    correctAnswerIndex: 1,
    concept: 'Polymorphism',
    difficulty: 'MEDIUM',
    courseOutcome: 'CO-2',
    explanation: "Dynamic method dispatch is Java's mechanism for implementing runtime polymorphism where the actual object instance type determines which overridden method executes."
  },
  {
    id: 'Q-104',
    questionText: 'Can an interface in Java 8 and later versions contain concrete method implementations without breaking backward compatibility?',
    options: [
      'No, interfaces can strictly contain abstract methods only',
      'Yes, using default and static methods',
      'Yes, but only if marked with the protected keyword',
      'Only if the interface extends java.lang.Cloneable'
    ],
    correctAnswerIndex: 1,
    concept: 'Interfaces',
    difficulty: 'MEDIUM',
    courseOutcome: 'CO-2',
    explanation: "Starting in Java 8, interfaces can define concrete behavior using 'default' and 'static' methods."
  },
  {
    id: 'Q-105',
    questionText: 'Which statement correctly describes how checked exceptions differ from unchecked exceptions in Java?',
    options: [
      'Checked exceptions inherit from java.lang.RuntimeException',
      'Checked exceptions must be either caught or declared in the throws clause at compile time',
      'Unchecked exceptions must always be caught in a try-finally block',
      'Checked exceptions can only be thrown by JVM internal errors'
    ],
    correctAnswerIndex: 1,
    concept: 'Exception Handling',
    difficulty: 'MEDIUM',
    courseOutcome: 'CO-3',
    explanation: 'Checked exceptions are verified at compile time and require handling via try-catch or declaration in the throws clause.'
  },
  {
    id: 'Q-106',
    questionText: 'In Java multithreading, which method transitions a Thread object from NEW to RUNNABLE state and initiates asynchronous execution?',
    options: ['run()', 'start()', 'execute()', 'resume()'],
    correctAnswerIndex: 1,
    concept: 'Multithreading',
    difficulty: 'EASY',
    courseOutcome: 'CO-3',
    explanation: 'Calling start() creates a new native OS thread of execution and invokes run() on that thread. Calling run() directly merely executes on the caller thread.'
  },
  {
    id: 'Q-107',
    questionText: 'When two distinct keys in a Java HashMap produce the same hashCode(), how does HashMap resolve the hash collision?',
    options: [
      'It throws a DuplicateKeyException',
      'It stores entries in a bucket using a linked list (or balanced red-black tree when threshold is reached)',
      'It replaces the first key with the second key automatically',
      'It shifts the secondary key to the next available linear bucket via linear probing'
    ],
    correctAnswerIndex: 1,
    concept: 'Collections & HashMap',
    difficulty: 'HARD',
    courseOutcome: 'CO-3',
    explanation: 'Java HashMap uses separate chaining. Colliding elements are stored in a bucket linked list; if the bin count exceeds 8, it converts to a red-black tree.'
  },
  {
    id: 'Q-108',
    questionText: "What is the primary purpose of the 'finally' block in Java exception handling?",
    options: [
      'To suppress any exception thrown in the try block',
      'To ensure cleanup code executes regardless of whether an exception was thrown',
      'To catch all Throwable instances including OutOfMemoryError',
      'To automatically restart the enclosing method upon failure'
    ],
    correctAnswerIndex: 1,
    concept: 'Exception Handling',
    difficulty: 'EASY',
    courseOutcome: 'CO-3',
    explanation: 'A finally block always executes after try/catch, making it ideal for deterministic resource cleanup.'
  },
  {
    id: 'Q-109',
    questionText: 'Which access modifier allows members of a class to be accessible within the same package and by subclasses in other packages?',
    options: ['private', 'package-private (default)', 'protected', 'public'],
    correctAnswerIndex: 2,
    concept: 'Packages & Encapsulation',
    difficulty: 'MEDIUM',
    courseOutcome: 'CO-2',
    explanation: "The 'protected' modifier grants access to other classes in the same package as well as any subclasses outside the package."
  },
  {
    id: 'Q-110',
    questionText: "What does Java's 'synchronized' keyword ensure when applied to a method?",
    options: [
      'The method executes faster by bypassing garbage collection',
      'Only one thread at a time can execute the method on the given monitor lock',
      'All variables modified inside are permanently saved to disk',
      'The method cannot throw any checked exceptions'
    ],
    correctAnswerIndex: 1,
    concept: 'Multithreading',
    difficulty: 'MEDIUM',
    courseOutcome: 'CO-3',
    explanation: 'Synchronized methods acquire the intrinsic monitor lock of the object, guaranteeing mutual exclusion.'
  }
];

const SEED_QUIZZES: Quiz[] = [
  {
    id: 'QUIZ-001',
    title: 'Java Core OOP & Concurrency Comprehensive Exam',
    description: 'Midterm assessment covering Inheritance, Polymorphism, Interfaces, Exception Handling, Collections, and Multithreading.',
    durationMinutes: 5,
    passPercentage: 60,
    courseOutcome: 'CO-1, CO-2, CO-3',
    questionIds: ['Q-101', 'Q-102', 'Q-103', 'Q-104', 'Q-105', 'Q-106', 'Q-107', 'Q-108', 'Q-109', 'Q-110'],
    instructions: [
      'The exam has a synchronized timer running on a separate thread.',
      'Each correct answer awards 1 mark. There is no negative marking.',
      'Answers can be changed anytime before final submission.',
      'When the countdown expires, answers are automatically submitted and evaluated.',
      'Following submission, access Question-Based Revision and Concept-Wise Performance Analytics.'
    ]
  },
  {
    id: 'QUIZ-002',
    title: 'Java Exception Handling & Multithreading Practice',
    description: 'Targeted assessment focusing on Course Outcome CO-3 topics including threads, locks, exceptions, and HashMap collisions.',
    durationMinutes: 3,
    passPercentage: 60,
    courseOutcome: 'CO-3',
    questionIds: ['Q-105', 'Q-106', 'Q-107', 'Q-108', 'Q-110'],
    instructions: [
      '3-minute timed examination with synchronized countdown.',
      'Covers Exception Handling and Multithreading.',
      'Automatic submission immediately upon timer expiry.'
    ]
  }
];

const SEED_ATTEMPTS: QuizAttempt[] = [
  {
    id: 'ATT-9001',
    quizId: 'QUIZ-001',
    quizTitle: 'Java Core OOP & Concurrency Comprehensive Exam',
    studentUsername: 'harsha_v',
    studentFullName: 'Harsha Vardhan E',
    totalQuestions: 10,
    attemptedCount: 9,
    unattemptedCount: 1,
    correctCount: 7,
    wrongCount: 2,
    score: 7,
    maxScore: 10,
    percentage: 70.0,
    startTime: '2026-10-10T11:00:00',
    submissionTime: '2026-10-10T11:04:12',
    timeTakenSeconds: 252,
    submissionType: 'MANUAL',
    snapshots: [
      {
        questionId: 'Q-101',
        questionText: 'In Java, what happens if a subclass constructor does not explicitly call super() and the superclass does not have a no-argument constructor?',
        options: [
          'The program compiles and initializes superclass fields to defaults',
          'A compile-time error occurs because super() is inserted implicitly but cannot be resolved',
          'A runtime NoSuchMethodError is thrown during instantiation',
          'The JVM automatically synthesizes an empty constructor in the superclass'
        ],
        correctAnswerIndex: 1,
        studentAnswerIndex: 0,
        concept: 'Inheritance',
        courseOutcome: 'CO-1',
        explanation: 'If a constructor does not explicitly invoke a superclass constructor, the Java compiler automatically inserts super(). If the superclass lacks an accessible no-arg constructor, compilation fails.',
        status: 'INCORRECT'
      },
      {
        questionId: 'Q-102',
        questionText: 'Which keyword is used by a subclass method to invoke an overridden method in its immediate superclass?',
        options: ['this', 'super', 'parent', 'base'],
        correctAnswerIndex: 1,
        studentAnswerIndex: 1,
        concept: 'Inheritance',
        courseOutcome: 'CO-1',
        explanation: "The 'super' keyword in Java is used to refer directly to the superclass instance members, allowing access to overridden methods.",
        status: 'CORRECT'
      },
      {
        questionId: 'Q-103',
        questionText: 'What is the mechanism by which a call to an overridden method is resolved at runtime rather than compile time in Java?',
        options: [
          'Static binding',
          'Dynamic Method Dispatch (Runtime Polymorphism)',
          'Method Overloading resolution',
          'Bytecode link preemption'
        ],
        correctAnswerIndex: 1,
        studentAnswerIndex: 1,
        concept: 'Polymorphism',
        courseOutcome: 'CO-2',
        explanation: 'Dynamic method dispatch is Java runtime polymorphism where the actual object instance type determines which overridden method executes.',
        status: 'CORRECT'
      },
      {
        questionId: 'Q-104',
        questionText: 'Can an interface in Java 8 and later versions contain concrete method implementations without breaking backward compatibility?',
        options: [
          'No, interfaces can strictly contain abstract methods only',
          'Yes, using default and static methods',
          'Yes, but only if marked with the protected keyword',
          'Only if the interface extends java.lang.Cloneable'
        ],
        correctAnswerIndex: 1,
        studentAnswerIndex: 1,
        concept: 'Interfaces',
        courseOutcome: 'CO-2',
        explanation: "Starting in Java 8, interfaces can define concrete behavior using 'default' and 'static' methods.",
        status: 'CORRECT'
      },
      {
        questionId: 'Q-105',
        questionText: 'Which statement correctly describes how checked exceptions differ from unchecked exceptions in Java?',
        options: [
          'Checked exceptions inherit from java.lang.RuntimeException',
          'Checked exceptions must be either caught or declared in the throws clause at compile time',
          'Unchecked exceptions must always be caught in a try-finally block',
          'Checked exceptions can only be thrown by JVM internal errors'
        ],
        correctAnswerIndex: 1,
        studentAnswerIndex: 1,
        concept: 'Exception Handling',
        courseOutcome: 'CO-3',
        explanation: 'Checked exceptions are verified at compile time and require handling via try-catch or declaration in the throws clause.',
        status: 'CORRECT'
      },
      {
        questionId: 'Q-106',
        questionText: 'In Java multithreading, which method transitions a Thread object from NEW to RUNNABLE state and initiates asynchronous execution?',
        options: ['run()', 'start()', 'execute()', 'resume()'],
        correctAnswerIndex: 1,
        studentAnswerIndex: 1,
        concept: 'Multithreading',
        courseOutcome: 'CO-3',
        explanation: 'Calling start() creates a new native OS thread of execution and invokes run() on that thread.',
        status: 'CORRECT'
      },
      {
        questionId: 'Q-107',
        questionText: 'When two distinct keys in a Java HashMap produce the same hashCode(), how does HashMap resolve the hash collision?',
        options: [
          'It throws a DuplicateKeyException',
          'It stores entries in a bucket using a linked list (or balanced red-black tree when threshold is reached)',
          'It replaces the first key with the second key automatically',
          'It shifts the secondary key to the next available linear bucket via linear probing'
        ],
        correctAnswerIndex: 1,
        studentAnswerIndex: 3,
        concept: 'Collections & HashMap',
        courseOutcome: 'CO-3',
        explanation: 'Java HashMap uses separate chaining. Colliding elements are stored in a bucket linked list; if the bin count exceeds 8, it converts to a red-black tree.',
        status: 'INCORRECT'
      },
      {
        questionId: 'Q-108',
        questionText: "What is the primary purpose of the 'finally' block in Java exception handling?",
        options: [
          'To suppress any exception thrown in the try block',
          'To ensure cleanup code executes regardless of whether an exception was thrown',
          'To catch all Throwable instances including OutOfMemoryError',
          'To automatically restart the enclosing method upon failure'
        ],
        correctAnswerIndex: 1,
        studentAnswerIndex: 1,
        concept: 'Exception Handling',
        courseOutcome: 'CO-3',
        explanation: 'A finally block always executes after try/catch, making it ideal for deterministic resource cleanup.',
        status: 'CORRECT'
      },
      {
        questionId: 'Q-109',
        questionText: 'Which access modifier allows members of a class to be accessible within the same package and by subclasses in other packages?',
        options: ['private', 'package-private (default)', 'protected', 'public'],
        correctAnswerIndex: 2,
        studentAnswerIndex: 2,
        concept: 'Packages & Encapsulation',
        courseOutcome: 'CO-2',
        explanation: "The 'protected' modifier grants access to other classes in the same package as well as any subclasses outside the package.",
        status: 'CORRECT'
      },
      {
        questionId: 'Q-110',
        questionText: "What does Java's 'synchronized' keyword ensure when applied to a method?",
        options: [
          'The method executes faster by bypassing garbage collection',
          'Only one thread at a time can execute the method on the given monitor lock',
          'All variables modified inside are permanently saved to disk',
          'The method cannot throw any checked exceptions'
        ],
        correctAnswerIndex: 1,
        studentAnswerIndex: -1,
        concept: 'Multithreading',
        courseOutcome: 'CO-3',
        explanation: 'Synchronized methods acquire the intrinsic monitor lock of the object, guaranteeing mutual exclusion.',
        status: 'UNATTEMPTED'
      }
    ]
  }
];

export default function App() {
  // Safe Persistent LocalStorage Storage Keys
  const STORAGE_KEY_USERS = 'alliance_quiz_v4_users';
  const STORAGE_KEY_QUESTIONS = 'alliance_quiz_v4_questions';
  const STORAGE_KEY_QUIZZES = 'alliance_quiz_v4_quizzes';
  const STORAGE_KEY_ATTEMPTS = 'alliance_quiz_v4_attempts';

  // State with LocalStorage re-hydration
  const [users, setUsers] = useState<User[]>(() => {
    const raw = localStorage.getItem(STORAGE_KEY_USERS);
    if (raw) {
      try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      } catch (e) { /* ignore */ }
    }
    return SEED_USERS;
  });

  const [questions, setQuestions] = useState<Question[]>(() => {
    const raw = localStorage.getItem(STORAGE_KEY_QUESTIONS);
    if (raw) {
      try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      } catch (e) { /* ignore */ }
    }
    return SEED_QUESTIONS;
  });

  const [quizzes, setQuizzes] = useState<Quiz[]>(() => {
    const raw = localStorage.getItem(STORAGE_KEY_QUIZZES);
    if (raw) {
      try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      } catch (e) { /* ignore */ }
    }
    return SEED_QUIZZES;
  });

  const [attempts, setAttempts] = useState<QuizAttempt[]>(() => {
    const raw = localStorage.getItem(STORAGE_KEY_ATTEMPTS);
    if (raw) {
      try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      } catch (e) { /* ignore */ }
    }
    return SEED_ATTEMPTS;
  });

  // Sync state to LocalStorage
  useEffect(() => {
    localStorage.setItem(STORAGE_KEY_USERS, JSON.stringify(users));
  }, [users]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY_QUESTIONS, JSON.stringify(questions));
  }, [questions]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY_QUIZZES, JSON.stringify(quizzes));
  }, [quizzes]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY_ATTEMPTS, JSON.stringify(attempts));
  }, [attempts]);

  // Auth & Session State: Strictly starts as null (NO AUTO-LOGIN BYPASS)
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [currentScreen, setCurrentScreen] = useState<string>('WELCOME');

  // Login Form State
  const [loginUsername, setLoginUsername] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loginError, setLoginError] = useState('');
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  // Student Registration Form State
  const [regFullName, setRegFullName] = useState('');
  const [regUsername, setRegUsername] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regDept, setRegDept] = useState('School of Advanced Computing (CS)');
  const [regPass, setRegPass] = useState('');
  const [regConfPass, setRegConfPass] = useState('');
  const [regSuccessMsg, setRegSuccessMsg] = useState('');
  const [regErrorMsg, setRegErrorMsg] = useState('');

  // Active Examination State
  const [activeQuiz, setActiveQuiz] = useState<Quiz | null>(null);
  const [examQuestions, setExamQuestions] = useState<Question[]>([]);
  const [currentQIndex, setCurrentQIndex] = useState<number>(0);
  const [studentAnswers, setStudentAnswers] = useState<Record<number, number>>({});
  const [flaggedQuestions, setFlaggedQuestions] = useState<Set<number>>(new Set());
  const [remainingSeconds, setRemainingSeconds] = useState<number>(0);
  const [isExamRunning, setIsExamRunning] = useState<boolean>(false);
  const [examStartTime, setExamStartTime] = useState<string>('');
  const [lastAttempt, setLastAttempt] = useState<QuizAttempt | null>(null);

  // Submission Dialog Modal State (Replaces blocked window.confirm)
  const [showSubmitModal, setShowSubmitModal] = useState<boolean>(false);

  // Delete Question Modal State (Replaces blocked window.confirm)
  const [questionToDelete, setQuestionToDelete] = useState<string | null>(null);

  // Question Creation Form State (Instructor)
  const [editingQuestion, setEditingQuestion] = useState<Question | null>(null);
  const [qConcept, setQConcept] = useState('Inheritance');
  const [qCO, setQCO] = useState('CO-1');
  const [qText, setQText] = useState('');
  const [qOptA, setQOptA] = useState('');
  const [qOptB, setQOptB] = useState('');
  const [qOptC, setQOptC] = useState('');
  const [qOptD, setQOptD] = useState('');
  const [qCorrectIdx, setQCorrectIdx] = useState(0);
  const [qExplanation, setQExplanation] = useState('');
  const [qDifficulty, setQDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD'>('MEDIUM');

  const handleOpenAddQuestion = () => {
    setEditingQuestion(null);
    setQConcept('Inheritance');
    setQCO('CO-1');
    setQDifficulty('MEDIUM');
    setQText('');
    setQOptA('');
    setQOptB('');
    setQOptC('');
    setQOptD('');
    setQCorrectIdx(0);
    setQExplanation('');
    navigateTo('ADD_QUESTION');
  };

  const handleOpenEditQuestion = (q: Question) => {
    setEditingQuestion(q);
    setQConcept(q.concept);
    setQCO(q.courseOutcome);
    setQDifficulty(q.difficulty);
    setQText(q.questionText);
    setQOptA(q.options[0] || '');
    setQOptB(q.options[1] || '');
    setQOptC(q.options[2] || '');
    setQOptD(q.options[3] || '');
    setQCorrectIdx(q.correctAnswerIndex);
    setQExplanation(q.explanation || '');
    navigateTo('ADD_QUESTION');
  };

  // Search & Filter State
  const [bankConceptFilter, setBankConceptFilter] = useState('ALL');
  const [bankSearch, setBankSearch] = useState('');

  // Notifications
  const [notification, setNotification] = useState<string | null>(null);

  const showNotification = (msg: string) => {
    setNotification(msg);
    setTimeout(() => setNotification(null), 3500);
  };

  // Safe Navigation with Role-Based Access Enforcement
  const navigateTo = (screen: string) => {
    // Instructor only pages
    if (['INSTRUCTOR_DASHBOARD', 'QUESTION_BANK', 'ADD_QUESTION', 'CREATE_QUIZ'].includes(screen)) {
      if (!currentUser || currentUser.role !== 'INSTRUCTOR') {
        showNotification('Access Denied: Instructor privileges required.');
        return;
      }
    }

    // Student / authenticated only pages
    if (['STUDENT_DASHBOARD', 'EXAM', 'QUIZ_INSTRUCTIONS'].includes(screen)) {
      if (!currentUser) {
        showNotification('Please sign in to access the examination portal.');
        setCurrentScreen('LOGIN');
        return;
      }
    }

    setCurrentScreen(screen);
  };

  // Synchronized Timer Effect
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isExamRunning && remainingSeconds > 0) {
      interval = setInterval(() => {
        setRemainingSeconds((prev) => {
          if (prev <= 1) {
            clearInterval(interval);
            // Automatic deadline timeout submission
            handleAutoSubmitOnTimeout();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [isExamRunning, remainingSeconds]);

  const formatTimer = (totalSec: number) => {
    const mins = Math.floor(totalSec / 60);
    const secs = totalSec % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // ==========================================================================
  // QUIZ SUBMISSION — AUTHORITATIVE REAL EVALUATION (HIGHEST PRIORITY)
  // ==========================================================================
  const handleStartExam = (quiz: Quiz) => {
    const qs = questions.filter((q) => quiz.questionIds.includes(q.id));
    if (qs.length === 0) {
      showNotification('Error: This examination has no questions associated with it.');
      return;
    }
    setActiveQuiz(quiz);
    setExamQuestions(qs);
    setCurrentQIndex(0);
    setStudentAnswers({});
    setFlaggedQuestions(new Set());
    setRemainingSeconds(quiz.durationMinutes * 60);
    setExamStartTime(new Date().toISOString());
    setIsExamRunning(true);
    setShowSubmitModal(false);
    setCurrentScreen('EXAM');
    showNotification(`Examination started: ${quiz.title}`);
  };

  // Automatic Timeout Submission (called when countdown hits 0)
  const handleAutoSubmitOnTimeout = () => {
    setShowSubmitModal(false);
    setIsExamRunning(false);
    finalizeSubmission('AUTOMATIC_TIMEOUT');
  };

  // Authoritative Submission Function (Evaluates, Scores, Preserves Snapshots, Persists)
  const finalizeSubmission = (submissionType: 'MANUAL' | 'AUTOMATIC_TIMEOUT') => {
    if (!activeQuiz || examQuestions.length === 0) return;

    setShowSubmitModal(false);
    setIsExamRunning(false);

    const total = examQuestions.length;
    let attempted = 0;
    let correct = 0;
    let wrong = 0;
    let unanswered = 0;
    let invalid = 0;

    // Create immutable historical snapshots
    const snapshots: QuestionSnapshot[] = examQuestions.map((q, idx) => {
      const selected = studentAnswers[idx] !== undefined ? studentAnswers[idx] : -1;
      let status: 'CORRECT' | 'INCORRECT' | 'UNANSWERED' | 'INVALID' = 'UNANSWERED';
      let savedAnswerIndex = -1;

      // 1. Validate question integrity
      const hasValidOptions = Array.isArray(q.options) && q.options.length > 0;
      const hasValidCorrectAnswer =
        hasValidOptions &&
        typeof q.correctAnswerIndex === 'number' &&
        q.correctAnswerIndex >= 0 &&
        q.correctAnswerIndex < q.options.length;

      // 2. Validate student selection against options and correct answer
      if (selected === -1 || selected === undefined || selected === null) {
        // Case 3: Unanswered
        status = 'UNANSWERED';
        savedAnswerIndex = -1;
        unanswered++;
      } else if (!hasValidOptions || !hasValidCorrectAnswer) {
        // Case 4: Missing or invalid question configuration
        status = 'INVALID';
        savedAnswerIndex = -1; // Prevent invalid values from being saved as valid answers
        invalid++;
      } else if (typeof selected !== 'number' || selected < -1 || selected >= q.options.length) {
        // Case 4: Selected option index out of bounds
        status = 'INVALID';
        savedAnswerIndex = -1; // Prevent invalid values from being saved as valid answers
        invalid++;
      } else if (selected === q.correctAnswerIndex) {
        // Case 1: Verified correct answer -> 1 mark awarded
        status = 'CORRECT';
        savedAnswerIndex = selected;
        attempted++;
        correct++;
      } else {
        // Case 2: Wrong answer -> 0 marks awarded
        status = 'INCORRECT';
        savedAnswerIndex = selected;
        attempted++;
        wrong++;
      }

      return {
        questionId: q.id,
        questionText: q.questionText,
        options: [...q.options],
        correctAnswerIndex: q.correctAnswerIndex,
        studentAnswerIndex: savedAnswerIndex,
        concept: q.concept,
        courseOutcome: q.courseOutcome,
        explanation: q.explanation,
        status
      };
    });

    const unattempted = unanswered;
    // Award 1 mark ONLY for verified correct answer; 0 for wrong, unanswered, or invalid answers
    const score = correct;
    const maxScore = total;
    const percentage = total > 0 ? Math.round((score / maxScore) * 10000) / 100 : 0;
    const timeTaken = Math.max(1, activeQuiz.durationMinutes * 60 - remainingSeconds);

    const newAttempt: QuizAttempt = {
      id: `ATT-${Math.floor(1000 + Math.random() * 9000)}`,
      quizId: activeQuiz.id,
      quizTitle: activeQuiz.title,
      studentUsername: currentUser?.username || 'anonymous',
      studentFullName: currentUser?.fullName || 'Student Candidate',
      totalQuestions: total,
      attemptedCount: attempted,
      unattemptedCount: unattempted,
      invalidCount: invalid,
      correctCount: correct,
      wrongCount: wrong,
      score,
      maxScore,
      percentage,
      startTime: examStartTime || new Date().toISOString(),
      submissionTime: new Date().toISOString(),
      timeTakenSeconds: timeTaken,
      submissionType,
      snapshots
    };

    // Save completed attempt to state and LocalStorage
    setAttempts((prev) => [newAttempt, ...prev]);
    setLastAttempt(newAttempt);
    setCurrentScreen('RESULTS');

    if (submissionType === 'AUTOMATIC_TIMEOUT') {
      showNotification(`Time expired: Exam auto-submitted. Score: ${score}/${maxScore} (${percentage}%)`);
    } else {
      showNotification(`Exam submitted successfully! Score: ${score}/${maxScore} (${percentage}%)`);
    }
  };

  // Concept Analytics Aggregator using HashMap
  const computeConceptAnalytics = () => {
    const relevantAttempts = currentUser?.role === 'INSTRUCTOR'
      ? attempts
      : attempts.filter((a) => a.studentUsername === currentUser?.username);

    const conceptMap: Record<
      string,
      {
        total: number;
        attempted: number;
        correct: number;
        wrong: number;
        unattempted: number;
      }
    > = {};

    relevantAttempts.forEach((attempt) => {
      attempt.snapshots.forEach((snap) => {
        const c = snap.concept || 'General Java Core';
        if (!conceptMap[c]) {
          conceptMap[c] = { total: 0, attempted: 0, correct: 0, wrong: 0, unattempted: 0, invalid: 0 };
        }
        conceptMap[c].total++;
        if (snap.status === 'CORRECT') {
          conceptMap[c].attempted++;
          conceptMap[c].correct++;
        } else if (snap.status === 'INCORRECT') {
          conceptMap[c].attempted++;
          conceptMap[c].wrong++;
        } else if (snap.status === 'INVALID') {
          conceptMap[c].invalid = (conceptMap[c].invalid || 0) + 1;
        } else {
          conceptMap[c].unattempted++;
        }
      });
    });

    return Object.entries(conceptMap).map(([concept, data]) => {
      const accuracy = data.attempted > 0 ? Math.round((data.correct / data.attempted) * 10000) / 100 : 0;
      let recommendation = 'Not attempted';
      let statusLevel = 'UNATTEMPTED';

      if (data.attempted > 0) {
        if (accuracy < 60) {
          recommendation = 'Needs revision';
          statusLevel = 'WEAK';
        } else if (accuracy < 80) {
          recommendation = 'Practice more';
          statusLevel = 'MODERATE';
        } else {
          recommendation = 'Good performance';
          statusLevel = 'STRONG';
        }
      }

      return {
        concept,
        ...data,
        accuracy,
        recommendation,
        statusLevel
      };
    });
  };

  // ==========================================================================
  // AUTHENTICATION: REAL CREDENTIAL VERIFICATION (NO BYPASS)
  // ==========================================================================
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError('');
    setIsLoggingIn(true);

    const cleanUser = loginUsername.trim().toLowerCase();
    const cleanPass = loginPassword.trim();

    if (!cleanUser || !cleanPass) {
      setLoginError('Both username and password are required.');
      setIsLoggingIn(false);
      return;
    }

    const found = users.find((u) => u.username.toLowerCase() === cleanUser);
    if (!found) {
      setLoginError('Invalid username or password.');
      setIsLoggingIn(false);
      return;
    }

    // Verify salted cryptographic hash
    const computedHash = await hashPassword(cleanPass, found.passwordSalt);
    if (computedHash !== found.passwordHash) {
      setLoginError('Invalid username or password.');
      setIsLoggingIn(false);
      return;
    }

    // Authentication succeeded
    setCurrentUser(found);
    setLoginPassword('');
    setIsLoggingIn(false);

    if (found.role === 'INSTRUCTOR') {
      setCurrentScreen('INSTRUCTOR_DASHBOARD');
    } else {
      setCurrentScreen('STUDENT_DASHBOARD');
    }
    showNotification(`Signed in successfully as ${found.fullName}`);
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setRegErrorMsg('');
    setRegSuccessMsg('');

    const cleanUsername = regUsername.trim().toLowerCase();
    const cleanFullName = regFullName.trim();
    const cleanEmail = regEmail.trim();

    if (cleanUsername.length < 3) {
      setRegErrorMsg('Username must be at least 3 characters long.');
      return;
    }
    if (!cleanFullName) {
      setRegErrorMsg('Full name cannot be empty.');
      return;
    }
    if (regPass.length < 6) {
      setRegErrorMsg('Password must be at least 6 characters.');
      return;
    }
    if (regPass !== regConfPass) {
      setRegErrorMsg('Passwords do not match.');
      return;
    }
    if (users.some((u) => u.username.toLowerCase() === cleanUsername)) {
      setRegErrorMsg(`Username '${cleanUsername}' is already registered.`);
      return;
    }

    const salt = generateSalt();
    const hash = await hashPassword(regPass, salt);

    const newUser: User = {
      id: `USR-${Math.floor(100 + Math.random() * 900)}`,
      username: cleanUsername,
      fullName: cleanFullName,
      email: cleanEmail,
      role: 'STUDENT',
      passwordSalt: salt,
      passwordHash: hash,
      department: regDept
    };

    setUsers((prev) => [...prev, newUser]);
    setRegSuccessMsg(`Registration complete! You may now sign in with your credentials.`);
    setRegFullName('');
    setRegUsername('');
    setRegEmail('');
    setRegPass('');
    setRegConfPass('');
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setCurrentScreen('WELCOME');
    showNotification('Signed out.');
  };

  return (
    <div className="flex flex-col h-screen bg-slate-900 text-slate-800 font-sans overflow-hidden">
      {/* Top Application Header */}
      <header className="bg-slate-950 border-b border-slate-800 px-6 py-3 flex items-center justify-between shrink-0 shadow">
        <div className="flex items-center space-x-3 cursor-pointer" onClick={() => navigateTo('WELCOME')}>
          <div className="w-9 h-9 rounded-lg bg-blue-700 flex items-center justify-center text-white shadow-sm">
            <GraduationCap className="w-5 h-5" />
          </div>
          <div>
            <div className="text-sm font-bold tracking-wide text-white flex items-center gap-2">
              ALLIANCE UNIVERSITY
              <span className="text-[11px] font-medium px-2 py-0.5 rounded bg-blue-900/70 text-blue-300 border border-blue-700/50">
                School of Advanced Computing
              </span>
            </div>
            <div className="text-xs text-slate-400">Online Quiz Management System</div>
          </div>
        </div>

        {/* Clean Primary Navigation */}
        <nav className="flex items-center space-x-1 text-xs">
          <button
            onClick={() => navigateTo('WELCOME')}
            className={`px-3 py-1.5 rounded font-medium transition ${
              currentScreen === 'WELCOME' ? 'bg-blue-600 text-white' : 'text-slate-300 hover:text-white hover:bg-slate-800'
            }`}
          >
            Home
          </button>

          {currentUser?.role === 'STUDENT' && (
            <>
              <button
                onClick={() => navigateTo('STUDENT_DASHBOARD')}
                className={`px-3 py-1.5 rounded font-medium transition ${
                  ['STUDENT_DASHBOARD', 'QUIZ_INSTRUCTIONS', 'EXAM'].includes(currentScreen)
                    ? 'bg-blue-600 text-white'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800'
                }`}
              >
                Quizzes
              </button>
              <button
                onClick={() => navigateTo('HISTORY')}
                className={`px-3 py-1.5 rounded font-medium transition ${
                  currentScreen === 'HISTORY' ? 'bg-blue-600 text-white' : 'text-slate-300 hover:text-white hover:bg-slate-800'
                }`}
              >
                History
              </button>
              <button
                onClick={() => navigateTo('CONCEPT_ANALYTICS')}
                className={`px-3 py-1.5 rounded font-medium transition ${
                  currentScreen === 'CONCEPT_ANALYTICS' ? 'bg-blue-600 text-white' : 'text-slate-300 hover:text-white hover:bg-slate-800'
                }`}
              >
                Concept Analytics
              </button>
            </>
          )}

          {currentUser?.role === 'INSTRUCTOR' && (
            <>
              <button
                onClick={() => navigateTo('INSTRUCTOR_DASHBOARD')}
                className={`px-3 py-1.5 rounded font-medium transition ${
                  currentScreen === 'INSTRUCTOR_DASHBOARD' ? 'bg-blue-600 text-white' : 'text-slate-300 hover:text-white hover:bg-slate-800'
                }`}
              >
                Dashboard
              </button>
              <button
                onClick={() => navigateTo('QUESTION_BANK')}
                className={`px-3 py-1.5 rounded font-medium transition ${
                  ['QUESTION_BANK', 'ADD_QUESTION'].includes(currentScreen)
                    ? 'bg-blue-600 text-white'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800'
                }`}
              >
                Question Bank
              </button>
              <button
                onClick={() => navigateTo('CREATE_QUIZ')}
                className={`px-3 py-1.5 rounded font-medium transition ${
                  currentScreen === 'CREATE_QUIZ' ? 'bg-blue-600 text-white' : 'text-slate-300 hover:text-white hover:bg-slate-800'
                }`}
              >
                Schedule Quiz
              </button>
              <button
                onClick={() => navigateTo('CONCEPT_ANALYTICS')}
                className={`px-3 py-1.5 rounded font-medium transition ${
                  currentScreen === 'CONCEPT_ANALYTICS' ? 'bg-blue-600 text-white' : 'text-slate-300 hover:text-white hover:bg-slate-800'
                }`}
              >
                Concept Analytics
              </button>
              <button
                onClick={() => navigateTo('HISTORY')}
                className={`px-3 py-1.5 rounded font-medium transition ${
                  currentScreen === 'HISTORY' ? 'bg-blue-600 text-white' : 'text-slate-300 hover:text-white hover:bg-slate-800'
                }`}
              >
                History
              </button>
            </>
          )}

          <button
            onClick={() => navigateTo('ABOUT')}
            className={`px-3 py-1.5 rounded font-medium transition ${
              currentScreen === 'ABOUT' ? 'bg-blue-600 text-white' : 'text-slate-300 hover:text-white hover:bg-slate-800'
            }`}
          >
            About
          </button>
        </nav>

        {/* User Session Profile & Controls */}
        <div className="flex items-center space-x-2 text-xs">
          {currentUser ? (
            <div className="flex items-center space-x-3 bg-slate-900 px-3 py-1.5 rounded-lg border border-slate-800">
              <UserIcon className="w-3.5 h-3.5 text-blue-400" />
              <div className="text-left">
                <span className="text-slate-200 font-semibold block">{currentUser.fullName}</span>
                <span className="text-[10px] text-slate-400">{currentUser.role}</span>
              </div>
              <button
                onClick={handleLogout}
                className="ml-2 px-2.5 py-1 bg-slate-800 hover:bg-red-900/60 text-slate-300 hover:text-red-200 rounded text-xs flex items-center gap-1 border border-slate-700 transition"
              >
                <LogOut className="w-3 h-3" />
                Sign Out
              </button>
            </div>
          ) : (
            <div className="flex items-center space-x-2">
              <button
                onClick={() => navigateTo('LOGIN')}
                className="px-3 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded font-medium text-xs shadow transition"
              >
                Sign In
              </button>
              <button
                onClick={() => navigateTo('REGISTER')}
                className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded font-medium text-xs border border-slate-700 transition"
              >
                Register
              </button>
            </div>
          )}
        </div>
      </header>

      {/* Global Notification Banner */}
      {notification && (
        <div className="bg-emerald-600 text-white text-xs px-4 py-2 flex items-center justify-between shadow z-50">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4" />
            <span>{notification}</span>
          </div>
          <button onClick={() => setNotification(null)} className="text-emerald-200 hover:text-white">
            ✕
          </button>
        </div>
      )}

      {/* Main Screen Body Viewport */}
      <main className="flex-1 overflow-y-auto bg-slate-100 p-6 flex flex-col justify-between">
        <div className="flex-1 flex flex-col justify-center">
          {/* ============================================================= */}
          {/* SCREEN: WELCOME / HOME                                        */}
          {/* ============================================================= */}
          {currentScreen === 'WELCOME' && (
            <div className="max-w-4xl mx-auto w-full py-4">
              <div className="text-center mb-8">
                <h2 className="text-2xl font-extrabold text-blue-950 tracking-tight">ALLIANCE UNIVERSITY</h2>
                <h3 className="text-base font-semibold text-slate-600">Alliance School of Advanced Computing</h3>
                <p className="text-sm font-medium text-teal-800 mt-1">
                  Online Quiz Management System with Synchronized Timer & Question-Based Revision
                </p>
                <div className="mt-3 inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-50 border border-blue-200 text-xs text-blue-800 font-medium">
                  Course Outcomes: CO-1 (OOP & Inheritance) • CO-2 (Polymorphism & Interfaces) • CO-3 (Exceptions & Multithreading)
                </div>
              </div>

              <div className="grid md:grid-cols-2 gap-6">
                {/* Student Card */}
                <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-sm flex flex-col justify-between hover:shadow-md transition">
                  <div>
                    <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-700 flex items-center justify-center mb-4">
                      <GraduationCap className="w-6 h-6" />
                    </div>
                    <h4 className="text-lg font-bold text-slate-900 mb-2">Student Portal</h4>
                    <p className="text-xs text-slate-600 leading-relaxed mb-6">
                      Participate in scheduled multiple-choice exams with an active synchronized countdown timer, automatic timeout submission, immediate scoring, question-based revision, and concept-wise analytics.
                    </p>
                  </div>
                  <div className="space-y-2 pt-4 border-t border-slate-100">
                    <button
                      onClick={() => {
                        if (currentUser?.role === 'STUDENT') {
                          navigateTo('STUDENT_DASHBOARD');
                        } else {
                          navigateTo('LOGIN');
                        }
                      }}
                      className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold shadow transition"
                    >
                      {currentUser?.role === 'STUDENT' ? 'Go to Quizzes Dashboard →' : 'Student Sign In →'}
                    </button>
                    {!currentUser && (
                      <button
                        onClick={() => navigateTo('REGISTER')}
                        className="w-full py-2 bg-slate-50 hover:bg-slate-100 text-slate-700 rounded-lg text-xs font-medium border border-slate-200 transition"
                      >
                        Register New Student Account
                      </button>
                    )}
                  </div>
                </div>

                {/* Instructor Card */}
                <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-sm flex flex-col justify-between hover:shadow-md transition">
                  <div>
                    <div className="w-12 h-12 rounded-xl bg-teal-50 text-teal-700 flex items-center justify-center mb-4">
                      <BookOpen className="w-6 h-6" />
                    </div>
                    <h4 className="text-lg font-bold text-slate-900 mb-2">Instructor Portal</h4>
                    <p className="text-xs text-slate-600 leading-relaxed mb-6">
                      Manage the Java question bank, tag questions with Java concepts, schedule examinations with timed deadlines, and evaluate cohort-wide concept weakness analytics using HashMaps.
                    </p>
                  </div>
                  <div className="space-y-2 pt-4 border-t border-slate-100">
                    <button
                      onClick={() => {
                        if (currentUser?.role === 'INSTRUCTOR') {
                          navigateTo('INSTRUCTOR_DASHBOARD');
                        } else {
                          navigateTo('LOGIN');
                        }
                      }}
                      className="w-full py-2.5 bg-teal-700 hover:bg-teal-800 text-white rounded-lg text-xs font-semibold shadow transition"
                    >
                      {currentUser?.role === 'INSTRUCTOR' ? 'Go to Instructor Console →' : 'Instructor Sign In →'}
                    </button>
                    <button
                      onClick={() => navigateTo('ABOUT')}
                      className="w-full py-2 bg-slate-50 hover:bg-slate-100 text-slate-700 rounded-lg text-xs font-medium border border-slate-200 transition"
                    >
                      View Micro-Project Proposal & Details
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: CLEAN, MODERN, CENTERED LOGIN                         */}
          {/* ============================================================= */}
          {currentScreen === 'LOGIN' && (
            <div className="max-w-md mx-auto w-full py-4">
              <div className="bg-white rounded-2xl p-8 border border-slate-200 shadow-lg">
                <div className="text-center mb-6">
                  <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-700 flex items-center justify-center mx-auto mb-3 shadow-sm border border-blue-100">
                    <ShieldCheck className="w-6 h-6" />
                  </div>
                  <h3 className="text-xl font-bold text-blue-950">Examination Portal Sign In</h3>
                  <p className="text-xs text-slate-500 mt-1">Alliance School of Advanced Computing</p>
                </div>

                {loginError && (
                  <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    <span>{loginError}</span>
                  </div>
                )}

                <form onSubmit={handleLogin} className="space-y-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1.5">Username</label>
                    <input
                      type="text"
                      required
                      autoFocus
                      value={loginUsername}
                      onChange={(e) => setLoginUsername(e.target.value)}
                      placeholder="e.g. harsha_v or instructor_admin"
                      className="w-full px-3.5 py-2.5 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none transition"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1.5">Password</label>
                    <div className="relative">
                      <input
                        type={showPassword ? 'text' : 'password'}
                        required
                        value={loginPassword}
                        onChange={(e) => setLoginPassword(e.target.value)}
                        placeholder="Enter password"
                        className="w-full px-3.5 py-2.5 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none pr-10 transition"
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600 transition"
                      >
                        {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>

                  <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-[11px] text-slate-600 flex items-start gap-2">
                    <HelpCircle className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-semibold text-slate-800">Verification Accounts:</span>
                      <div>• Student: <span className="font-mono text-blue-800 font-semibold">harsha_v</span> (Password: <span className="font-mono">Password123</span>)</div>
                      <div>• Faculty: <span className="font-mono text-teal-800 font-semibold">instructor_admin</span> (Password: <span className="font-mono">Password123</span>)</div>
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={isLoggingIn}
                    className="w-full mt-2 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-lg font-semibold text-xs shadow-md transition"
                  >
                    {isLoggingIn ? 'Verifying Credentials...' : 'Sign In'}
                  </button>
                </form>

                <div className="mt-6 pt-5 border-t border-slate-100 flex items-center justify-between text-xs text-slate-600">
                  <span>Don&apos;t have an account?</span>
                  <button
                    onClick={() => navigateTo('REGISTER')}
                    className="font-semibold text-blue-600 hover:underline"
                  >
                    Register as Student →
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: STUDENT REGISTRATION                                 */}
          {/* ============================================================= */}
          {currentScreen === 'REGISTER' && (
            <div className="max-w-md mx-auto w-full py-4">
              <div className="bg-white rounded-2xl p-8 border border-slate-200 shadow-lg">
                <div className="text-center mb-5">
                  <h3 className="text-xl font-bold text-blue-950">Student Account Registration</h3>
                  <p className="text-xs text-slate-500 mt-1">Join CS204 Advanced Java Examination Portal</p>
                </div>

                {regErrorMsg && (
                  <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    <span>{regErrorMsg}</span>
                  </div>
                )}
                {regSuccessMsg && (
                  <div className="mb-4 p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 shrink-0" />
                    <span>{regSuccessMsg}</span>
                  </div>
                )}

                <form onSubmit={handleRegister} className="space-y-3.5">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Full Name</label>
                    <input
                      type="text"
                      required
                      value={regFullName}
                      onChange={(e) => setRegFullName(e.target.value)}
                      placeholder="e.g. K Dheeraj Chowdary"
                      className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Username</label>
                    <input
                      type="text"
                      required
                      value={regUsername}
                      onChange={(e) => setRegUsername(e.target.value)}
                      placeholder="e.g. dheeraj_k"
                      className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Alliance University Email</label>
                    <input
                      type="email"
                      required
                      value={regEmail}
                      onChange={(e) => setRegEmail(e.target.value)}
                      placeholder="student@ced.alliance.edu.in"
                      className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Department</label>
                    <select
                      value={regDept}
                      onChange={(e) => setRegDept(e.target.value)}
                      className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none bg-white"
                    >
                      <option>School of Advanced Computing (CS)</option>
                      <option>Information Science & Engineering</option>
                      <option>Artificial Intelligence & ML</option>
                      <option>Data Science & Analytics</option>
                    </select>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1">Password</label>
                      <input
                        type="password"
                        required
                        value={regPass}
                        onChange={(e) => setRegPass(e.target.value)}
                        placeholder="Min 6 characters"
                        className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1">Confirm Password</label>
                      <input
                        type="password"
                        required
                        value={regConfPass}
                        onChange={(e) => setRegConfPass(e.target.value)}
                        placeholder="Retype password"
                        className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    className="w-full mt-3 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-semibold text-xs shadow-md transition"
                  >
                    Create Student Account
                  </button>
                </form>

                <div className="mt-5 pt-4 border-t border-slate-100 text-center">
                  <button
                    onClick={() => navigateTo('LOGIN')}
                    className="text-xs text-blue-600 font-semibold hover:underline"
                  >
                    Already registered? Sign In →
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: STUDENT DASHBOARD                                     */}
          {/* ============================================================= */}
          {currentScreen === 'STUDENT_DASHBOARD' && (
            <div className="max-w-5xl mx-auto w-full space-y-6 py-2">
              <div className="bg-gradient-to-r from-blue-900 to-indigo-800 rounded-2xl p-6 text-white shadow-md flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <span className="text-xs font-medium px-2.5 py-0.5 rounded-full bg-blue-800 text-blue-200">
                    Student Portal
                  </span>
                  <h2 className="text-xl font-bold mt-1.5">Welcome, {currentUser?.fullName}</h2>
                  <p className="text-xs text-blue-200 mt-0.5">{currentUser?.department}</p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => navigateTo('CONCEPT_ANALYTICS')}
                    className="px-3.5 py-2 bg-white/10 hover:bg-white/20 border border-white/20 rounded-lg text-xs font-medium flex items-center gap-1.5 transition"
                  >
                    <BarChart3 className="w-4 h-4" />
                    Concept Analytics
                  </button>
                  <button
                    onClick={() => navigateTo('HISTORY')}
                    className="px-3.5 py-2 bg-white/10 hover:bg-white/20 border border-white/20 rounded-lg text-xs font-medium flex items-center gap-1.5 transition"
                  >
                    <Award className="w-4 h-4" />
                    Past Attempts
                  </button>
                </div>
              </div>

              <div>
                <h3 className="text-sm font-bold text-slate-800 tracking-wide uppercase mb-3">
                  Available Scheduled Examinations
                </h3>

                <div className="grid md:grid-cols-2 gap-5">
                  {quizzes.map((quiz) => (
                    <div
                      key={quiz.id}
                      className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm hover:border-blue-300 transition flex flex-col justify-between"
                    >
                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-xs font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                            {quiz.id}
                          </span>
                          <span className="text-xs font-medium text-slate-500 flex items-center gap-1">
                            <Clock className="w-3.5 h-3.5 text-blue-600" />
                            {quiz.durationMinutes} Minutes Synchronized
                          </span>
                        </div>
                        <h4 className="text-base font-bold text-slate-900 mb-1.5">{quiz.title}</h4>
                        <p className="text-xs text-slate-600 mb-4 leading-relaxed">{quiz.description}</p>

                        <div className="flex flex-wrap gap-2 text-xs mb-4">
                          <span className="bg-slate-100 text-slate-700 px-2.5 py-1 rounded font-medium">
                            {quiz.questionIds.length} Questions
                          </span>
                          <span className="bg-slate-100 text-slate-700 px-2.5 py-1 rounded font-medium">
                            Pass Mark: {quiz.passPercentage}%
                          </span>
                          <span className="bg-emerald-50 text-emerald-800 px-2.5 py-1 rounded border border-emerald-200 font-medium">
                            {quiz.courseOutcome}
                          </span>
                        </div>
                      </div>

                      <button
                        onClick={() => {
                          setActiveQuiz(quiz);
                          navigateTo('QUIZ_INSTRUCTIONS');
                        }}
                        className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-bold flex items-center justify-center gap-1.5 transition shadow"
                      >
                        <span>Start Examination</span>
                        <ChevronRight className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: INSTRUCTOR DASHBOARD                                  */}
          {/* ============================================================= */}
          {currentScreen === 'INSTRUCTOR_DASHBOARD' && (
            <div className="max-w-5xl mx-auto w-full space-y-6 py-2">
              <div className="bg-gradient-to-r from-teal-900 to-slate-800 rounded-2xl p-6 text-white shadow-md flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                  <span className="text-xs font-medium px-2.5 py-0.5 rounded-full bg-teal-800 text-teal-200">
                    Faculty Administration Console
                  </span>
                  <h2 className="text-xl font-bold mt-1.5">{currentUser?.fullName}</h2>
                  <p className="text-xs text-teal-200 mt-0.5">{currentUser?.department}</p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => navigateTo('QUESTION_BANK')}
                    className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow transition"
                  >
                    <Plus className="w-4 h-4" />
                    Manage Questions ({questions.length})
                  </button>
                  <button
                    onClick={() => navigateTo('CREATE_QUIZ')}
                    className="px-3.5 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow transition"
                  >
                    <Plus className="w-4 h-4" />
                    Schedule Exam
                  </button>
                </div>
              </div>

              <div className="grid md:grid-cols-2 gap-5">
                <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
                  <div>
                    <div className="w-10 h-10 rounded-lg bg-teal-50 text-teal-700 flex items-center justify-center mb-3">
                      <BookOpen className="w-5 h-5" />
                    </div>
                    <h4 className="text-base font-bold text-slate-900 mb-1">Question Bank Management</h4>
                    <p className="text-xs text-slate-600 leading-relaxed mb-4">
                      Create, edit, and organize multiple-choice questions tagged with Java concepts (Inheritance, Polymorphism, Interfaces, Exception Handling, Multithreading).
                    </p>
                  </div>
                  <button
                    onClick={() => navigateTo('QUESTION_BANK')}
                    className="w-full py-2 bg-slate-50 hover:bg-slate-100 text-slate-800 rounded-lg text-xs font-semibold border border-slate-200 transition"
                  >
                    Open Question Bank ({questions.length} items) →
                  </button>
                </div>

                <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
                  <div>
                    <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center mb-3">
                      <Clock className="w-5 h-5" />
                    </div>
                    <h4 className="text-base font-bold text-slate-900 mb-1">Schedule Examination</h4>
                    <p className="text-xs text-slate-600 leading-relaxed mb-4">
                      Configure new timed exams, select questions, set passing percentage thresholds, and activate tests for students.
                    </p>
                  </div>
                  <button
                    onClick={() => navigateTo('CREATE_QUIZ')}
                    className="w-full py-2 bg-slate-50 hover:bg-slate-100 text-slate-800 rounded-lg text-xs font-semibold border border-slate-200 transition"
                  >
                    Create New Quiz →
                  </button>
                </div>

                <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
                  <div>
                    <div className="w-10 h-10 rounded-lg bg-purple-50 text-purple-700 flex items-center justify-center mb-3">
                      <BarChart3 className="w-5 h-5" />
                    </div>
                    <h4 className="text-base font-bold text-slate-900 mb-1">Concept Weakness Analytics</h4>
                    <p className="text-xs text-slate-600 leading-relaxed mb-4">
                      Analyze aggregate mistakes across Java concepts using HashMap logic. Identify which topics have &lt;60% accuracy requiring revision lectures.
                    </p>
                  </div>
                  <button
                    onClick={() => navigateTo('CONCEPT_ANALYTICS')}
                    className="w-full py-2 bg-slate-50 hover:bg-slate-100 text-slate-800 rounded-lg text-xs font-semibold border border-slate-200 transition"
                  >
                    View Concept Analytics →
                  </button>
                </div>

                <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between">
                  <div>
                    <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center mb-3">
                      <Award className="w-5 h-5" />
                    </div>
                    <h4 className="text-base font-bold text-slate-900 mb-1">Student Exam Submissions</h4>
                    <p className="text-xs text-slate-600 leading-relaxed mb-4">
                      Inspect student attempts, verify scores, review timestamps, and examine historical question snapshots.
                    </p>
                  </div>
                  <button
                    onClick={() => navigateTo('HISTORY')}
                    className="w-full py-2 bg-slate-50 hover:bg-slate-100 text-slate-800 rounded-lg text-xs font-semibold border border-slate-200 transition"
                  >
                    Review All Submissions ({attempts.length}) →
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: QUESTION BANK (INSTRUCTOR ONLY)                       */}
          {/* ============================================================= */}
          {currentScreen === 'QUESTION_BANK' && (
            <div className="max-w-5xl mx-auto w-full space-y-4 py-2">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h3 className="text-lg font-bold text-slate-900">Java Concept Question Bank</h3>
                  <p className="text-xs text-slate-500">
                    {questions.length} questions mapped to concepts and Alliance Course Outcomes
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={handleOpenAddQuestion}
                    className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow"
                  >
                    <Plus className="w-4 h-4" />
                    Add Question
                  </button>
                  <button
                    onClick={() => navigateTo('INSTRUCTOR_DASHBOARD')}
                    className="px-3 py-2 bg-slate-200 hover:bg-slate-300 text-slate-700 rounded-lg text-xs font-medium"
                  >
                    Back
                  </button>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-3 bg-white p-3 rounded-xl border border-slate-200">
                <Search className="w-4 h-4 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search question text or concept..."
                  value={bankSearch}
                  onChange={(e) => setBankSearch(e.target.value)}
                  className="px-2 py-1 text-xs border border-slate-300 rounded-lg flex-1 min-w-[150px] focus:outline-none"
                />
                <select
                  value={bankConceptFilter}
                  onChange={(e) => setBankConceptFilter(e.target.value)}
                  className="px-2.5 py-1 text-xs border border-slate-300 rounded-lg bg-white text-slate-700"
                >
                  <option value="ALL">All Java Concepts</option>
                  <option value="Inheritance">Inheritance</option>
                  <option value="Polymorphism">Polymorphism</option>
                  <option value="Interfaces">Interfaces</option>
                  <option value="Exception Handling">Exception Handling</option>
                  <option value="Multithreading">Multithreading</option>
                  <option value="Collections & HashMap">Collections & HashMap</option>
                  <option value="Packages & Encapsulation">Packages & Encapsulation</option>
                </select>
              </div>

              <div className="space-y-3">
                {questions
                  .filter((q) => {
                    const matchConcept = bankConceptFilter === 'ALL' || q.concept === bankConceptFilter;
                    const matchSearch =
                      !bankSearch ||
                      q.questionText.toLowerCase().includes(bankSearch.toLowerCase()) ||
                      q.concept.toLowerCase().includes(bankSearch.toLowerCase());
                    return matchConcept && matchSearch;
                  })
                  .map((q) => (
                    <div key={q.id} className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold text-slate-600 bg-slate-100 px-2 py-0.5 rounded">
                            {q.id}
                          </span>
                          <span className="text-xs font-semibold px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                            {q.concept}
                          </span>
                          <span className="text-xs px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 border border-emerald-200 font-medium">
                            {q.courseOutcome}
                          </span>
                        </div>
                        <div className="flex items-center gap-1">
                          <button
                            onClick={() => handleOpenEditQuestion(q)}
                            className="text-slate-400 hover:text-blue-600 p-1.5 rounded hover:bg-slate-100 transition"
                            title="Edit Question"
                          >
                            <Edit3 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => setQuestionToDelete(q.id)}
                            className="text-slate-400 hover:text-red-600 p-1.5 rounded hover:bg-slate-100 transition"
                            title="Delete Question"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                      <p className="text-xs font-semibold text-slate-900 mb-2">{q.questionText}</p>
                      <div className="grid sm:grid-cols-2 gap-2 text-xs mb-2">
                        {q.options.map((opt, oIdx) => (
                          <div
                            key={oIdx}
                            className={`p-2 rounded-lg text-xs ${
                              oIdx === q.correctAnswerIndex
                                ? 'bg-emerald-50 text-emerald-800 font-semibold border border-emerald-300'
                                : 'bg-slate-50 text-slate-600'
                            }`}
                          >
                            {String.fromCharCode(65 + oIdx)}) {opt}
                            {oIdx === q.correctAnswerIndex && ' ✓ (Correct)'}
                          </div>
                        ))}
                      </div>
                      <div className="text-[11px] text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-200">
                        <span className="font-semibold text-slate-800">Rationale: </span>
                        {q.explanation}
                      </div>
                    </div>
                  ))}
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: ADD / EDIT QUESTION (INSTRUCTOR ONLY)                 */}
          {/* ============================================================= */}
          {currentScreen === 'ADD_QUESTION' && (
            <div className="max-w-2xl mx-auto w-full py-2">
              <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-4">
                <h3 className="text-lg font-bold text-slate-900">
                  {editingQuestion ? `Edit Question (${editingQuestion.id})` : 'Add Question to Bank'}
                </h3>

                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Java Concept</label>
                    <select
                      value={qConcept}
                      onChange={(e) => setQConcept(e.target.value)}
                      className="w-full px-2.5 py-1.5 text-xs border border-slate-300 rounded-lg bg-white"
                    >
                      <option>Inheritance</option>
                      <option>Polymorphism</option>
                      <option>Interfaces</option>
                      <option>Exception Handling</option>
                      <option>Multithreading</option>
                      <option>Collections & HashMap</option>
                      <option>Packages & Encapsulation</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Course Outcome</label>
                    <select
                      value={qCO}
                      onChange={(e) => setQCO(e.target.value)}
                      className="w-full px-2.5 py-1.5 text-xs border border-slate-300 rounded-lg bg-white"
                    >
                      <option value="CO-1">CO-1 (OOP & Inheritance)</option>
                      <option value="CO-2">CO-2 (Polymorphism & Interfaces)</option>
                      <option value="CO-3">CO-3 (Exceptions & Threads)</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Difficulty</label>
                    <select
                      value={qDifficulty}
                      onChange={(e) => setQDifficulty(e.target.value as any)}
                      className="w-full px-2.5 py-1.5 text-xs border border-slate-300 rounded-lg bg-white"
                    >
                      <option value="EASY">EASY</option>
                      <option value="MEDIUM">MEDIUM</option>
                      <option value="HARD">HARD</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Question Prompt</label>
                  <textarea
                    rows={3}
                    value={qText}
                    onChange={(e) => setQText(e.target.value)}
                    placeholder="Enter question prompt..."
                    className="w-full p-2.5 text-xs border border-slate-300 rounded-lg focus:ring-1 focus:ring-blue-500 focus:outline-none"
                  />
                </div>

                <div className="space-y-2">
                  <label className="block text-xs font-semibold text-slate-700">Answer Options</label>
                  {[
                    { val: qOptA, set: setQOptA, label: 'A' },
                    { val: qOptB, set: setQOptB, label: 'B' },
                    { val: qOptC, set: setQOptC, label: 'C' },
                    { val: qOptD, set: setQOptD, label: 'D' }
                  ].map((item, idx) => (
                    <div key={idx} className="flex items-center gap-2">
                      <span className="w-5 text-xs font-bold text-slate-600">{item.label})</span>
                      <input
                        type="text"
                        value={item.val}
                        onChange={(e) => item.set(e.target.value)}
                        placeholder={`Option ${item.label}`}
                        className="flex-1 px-3 py-1.5 text-xs border border-slate-300 rounded-lg"
                      />
                      <label className="flex items-center gap-1.5 text-xs text-slate-600 cursor-pointer">
                        <input
                          type="radio"
                          name="correctOption"
                          checked={qCorrectIdx === idx}
                          onChange={() => setQCorrectIdx(idx)}
                        />
                        <span>Correct</span>
                      </label>
                    </div>
                  ))}
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Explanation / Concept Rationale</label>
                  <input
                    type="text"
                    value={qExplanation}
                    onChange={(e) => setQExplanation(e.target.value)}
                    placeholder="Explanation shown in revision mode..."
                    className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg"
                  />
                </div>

                <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                  <button
                    onClick={() => {
                      setEditingQuestion(null);
                      navigateTo('QUESTION_BANK');
                    }}
                    className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={() => {
                      if (!qText.trim() || !qOptA.trim() || !qOptB.trim()) {
                        showNotification('Please provide question text and at least options A and B.');
                        return;
                      }
                      if (editingQuestion) {
                        const updatedQ: Question = {
                          ...editingQuestion,
                          questionText: qText.trim(),
                          options: [qOptA.trim(), qOptB.trim(), qOptC.trim() || 'None of the above', qOptD.trim() || 'All of the above'],
                          correctAnswerIndex: qCorrectIdx,
                          concept: qConcept,
                          difficulty: qDifficulty,
                          courseOutcome: qCO,
                          explanation: qExplanation.trim() || 'Standard Java language specification behavior.'
                        };
                        setQuestions(questions.map((q) => (q.id === editingQuestion.id ? updatedQ : q)));
                        showNotification(`Question ${editingQuestion.id} updated successfully.`);
                        setEditingQuestion(null);
                      } else {
                        const newQ: Question = {
                          id: `Q-${Math.floor(200 + Math.random() * 800)}`,
                          questionText: qText.trim(),
                          options: [qOptA.trim(), qOptB.trim(), qOptC.trim() || 'None of the above', qOptD.trim() || 'All of the above'],
                          correctAnswerIndex: qCorrectIdx,
                          concept: qConcept,
                          difficulty: qDifficulty,
                          courseOutcome: qCO,
                          explanation: qExplanation.trim() || 'Standard Java language specification behavior.'
                        };
                        setQuestions([...questions, newQ]);
                        showNotification(`Question ${newQ.id} added.`);
                      }
                      navigateTo('QUESTION_BANK');
                    }}
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow"
                  >
                    {editingQuestion ? 'Update Question' : 'Save Question'}
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: CREATE QUIZ (INSTRUCTOR ONLY)                         */}
          {/* ============================================================= */}
          {currentScreen === 'CREATE_QUIZ' && (
            <div className="max-w-xl mx-auto w-full py-2">
              <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-4">
                <h3 className="text-lg font-bold text-slate-900">Schedule Examination</h3>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Quiz Title</label>
                  <input
                    type="text"
                    defaultValue="Java Core OOP Midterm Examination"
                    id="schedTitle"
                    className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Duration (Minutes)</label>
                    <input
                      type="number"
                      defaultValue={5}
                      id="schedDur"
                      className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Passing Mark (%)</label>
                    <input
                      type="number"
                      defaultValue={60}
                      id="schedPass"
                      className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Description</label>
                  <input
                    type="text"
                    defaultValue="Covers Course Outcomes CO-1, CO-2, and CO-3."
                    id="schedDesc"
                    className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg"
                  />
                </div>

                <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                  <button
                    onClick={() => navigateTo('INSTRUCTOR_DASHBOARD')}
                    className="px-4 py-2 bg-slate-100 text-slate-700 rounded-lg text-xs"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={() => {
                      const tEl = document.getElementById('schedTitle') as HTMLInputElement;
                      const dEl = document.getElementById('schedDur') as HTMLInputElement;
                      const pEl = document.getElementById('schedPass') as HTMLInputElement;
                      const dsEl = document.getElementById('schedDesc') as HTMLInputElement;

                      const newQuiz: Quiz = {
                        id: `QUIZ-${Math.floor(100 + Math.random() * 900)}`,
                        title: tEl?.value || 'Java Exam',
                        description: dsEl?.value || 'Timed exam.',
                        durationMinutes: parseInt(dEl?.value) || 5,
                        passPercentage: parseInt(pEl?.value) || 60,
                        courseOutcome: 'CO-1, CO-2, CO-3',
                        questionIds: questions.slice(0, 10).map((q) => q.id),
                        instructions: [
                          'The exam has a synchronized timer running on a separate thread.',
                          'Answers are auto-submitted upon timeout.',
                          'Exact snapshots preserved for revision.'
                        ]
                      };

                      setQuizzes([...quizzes, newQuiz]);
                      showNotification(`Quiz ${newQuiz.id} scheduled successfully.`);
                      navigateTo('INSTRUCTOR_DASHBOARD');
                    }}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold shadow"
                  >
                    Schedule Quiz
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: QUIZ INSTRUCTIONS (STUDENT)                          */}
          {/* ============================================================= */}
          {currentScreen === 'QUIZ_INSTRUCTIONS' && activeQuiz && (
            <div className="max-w-2xl mx-auto w-full py-4">
              <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-4">
                <div className="border-b border-slate-100 pb-3">
                  <span className="text-xs font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                    {activeQuiz.id}
                  </span>
                  <h3 className="text-xl font-bold text-slate-900 mt-2">{activeQuiz.title}</h3>
                  <p className="text-xs text-slate-600 mt-0.5">{activeQuiz.description}</p>
                </div>

                <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-xs text-amber-900 space-y-1.5">
                  <div className="font-bold flex items-center gap-1.5 text-amber-950">
                    <AlertCircle className="w-4 h-4 text-amber-700" />
                    Synchronized Timer & Auto-Submission Rules:
                  </div>
                  <p>• Dedicated countdown timer will run for <strong>{activeQuiz.durationMinutes} minutes</strong>.</p>
                  <p>• If the timer reaches 00:00, all answered questions are automatically submitted and evaluated.</p>
                  <p>• Each correct answer awards 1 mark. There is no negative marking.</p>
                  <p>• Answers can be changed anytime before clicking Submit Exam or deadline expiry.</p>
                </div>

                <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 text-xs text-slate-700">
                  <div className="font-bold text-slate-900 mb-1">Question-Based Revision Innovation:</div>
                  <p>
                    Following submission, every exact question snapshot will be preserved for your review, and concept-wise analytics will highlight your strengths and topics requiring revision.
                  </p>
                </div>

                <div className="pt-2 flex justify-between items-center">
                  <button
                    onClick={() => navigateTo('STUDENT_DASHBOARD')}
                    className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium"
                  >
                    ← Back to Quizzes
                  </button>
                  <button
                    onClick={() => handleStartExam(activeQuiz)}
                    className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold shadow transition"
                  >
                    Begin Examination →
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: ACTIVE EXAMINATION (WITH SYNCHRONIZED TIMER)          */}
          {/* ============================================================= */}
          {currentScreen === 'EXAM' && activeQuiz && (
            <div className="max-w-5xl mx-auto w-full grid md:grid-cols-4 gap-4 py-2">
              <div className="md:col-span-3 space-y-4">
                {/* Header card with Live Timer */}
                <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm flex items-center justify-between">
                  <div>
                    <h4 className="text-sm font-bold text-slate-900">{activeQuiz.title}</h4>
                    <div className="text-[11px] text-slate-500">
                      Candidate: <span className="font-semibold text-slate-800">{currentUser?.fullName}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="bg-red-50 border border-red-200 text-red-700 px-3.5 py-1.5 rounded-lg flex items-center gap-2 font-mono text-sm font-bold shadow-sm">
                      <Clock className="w-4 h-4 text-red-600 animate-pulse" />
                      <span>{formatTimer(remainingSeconds)}</span>
                    </div>
                  </div>
                </div>

                {/* Question Area */}
                {examQuestions[currentQIndex] && (
                  <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-sm space-y-4">
                    <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold px-2 py-0.5 rounded bg-blue-100 text-blue-900">
                          Question {currentQIndex + 1} of {examQuestions.length}
                        </span>
                        <span className="text-xs font-semibold px-2 py-0.5 rounded bg-teal-50 text-teal-700 border border-teal-200">
                          {examQuestions[currentQIndex].concept}
                        </span>
                        <span className="text-xs text-slate-500">
                          ({examQuestions[currentQIndex].courseOutcome})
                        </span>
                      </div>

                      <button
                        onClick={() => {
                          const nextFlagged = new Set(flaggedQuestions);
                          if (nextFlagged.has(currentQIndex)) {
                            nextFlagged.delete(currentQIndex);
                          } else {
                            nextFlagged.add(currentQIndex);
                          }
                          setFlaggedQuestions(nextFlagged);
                        }}
                        className={`text-xs px-2.5 py-1 rounded-lg flex items-center gap-1 font-medium transition ${
                          flaggedQuestions.has(currentQIndex)
                            ? 'bg-amber-100 text-amber-900 border border-amber-300'
                            : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                        }`}
                      >
                        <Flag className="w-3.5 h-3.5" />
                        <span>{flaggedQuestions.has(currentQIndex) ? 'Flagged' : 'Flag for Review'}</span>
                      </button>
                    </div>

                    <p className="text-sm font-semibold text-slate-900 leading-relaxed">
                      {examQuestions[currentQIndex].questionText}
                    </p>

                    <div className="space-y-2.5 pt-2">
                      {examQuestions[currentQIndex].options.map((option, optIdx) => {
                        const isSelected = studentAnswers[currentQIndex] === optIdx;
                        return (
                          <button
                            key={optIdx}
                            onClick={() => {
                              setStudentAnswers({
                                ...studentAnswers,
                                [currentQIndex]: optIdx
                              });
                            }}
                            className={`w-full p-3 rounded-xl border text-left text-xs font-medium transition flex items-center justify-between ${
                              isSelected
                                ? 'bg-blue-50 border-blue-500 text-blue-900 shadow-sm'
                                : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
                            }`}
                          >
                            <div className="flex items-center gap-3">
                              <span
                                className={`w-5 h-5 rounded-full flex items-center justify-center text-[11px] font-bold ${
                                  isSelected ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-600'
                                }`}
                              >
                                {String.fromCharCode(65 + optIdx)}
                              </span>
                              <span>{option}</span>
                            </div>
                            {isSelected && <CheckCircle2 className="w-4 h-4 text-blue-600" />}
                          </button>
                        );
                      })}
                    </div>

                    <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
                      <button
                        disabled={currentQIndex === 0}
                        onClick={() => setCurrentQIndex(currentQIndex - 1)}
                        className="px-3.5 py-1.5 bg-slate-100 hover:bg-slate-200 disabled:opacity-40 text-slate-700 rounded-lg text-xs font-medium flex items-center gap-1"
                      >
                        <ChevronLeft className="w-4 h-4" />
                        Previous
                      </button>

                      <div className="text-[11px] text-slate-500 font-medium">
                        {studentAnswers[currentQIndex] !== undefined ? (
                          <span className="text-emerald-700 font-bold">Answer Recorded</span>
                        ) : (
                          <span className="text-slate-400">Unanswered</span>
                        )}
                      </div>

                      <button
                        disabled={currentQIndex === examQuestions.length - 1}
                        onClick={() => setCurrentQIndex(currentQIndex + 1)}
                        className="px-3.5 py-1.5 bg-slate-100 hover:bg-slate-200 disabled:opacity-40 text-slate-700 rounded-lg text-xs font-medium flex items-center gap-1"
                      >
                        Next
                        <ChevronRight className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Palette */}
              <div className="space-y-4">
                <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm">
                  <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider mb-2">
                    Question Palette
                  </h4>

                  <div className="grid grid-cols-5 gap-1.5 mb-4">
                    {examQuestions.map((_, idx) => {
                      const isAnswered = studentAnswers[idx] !== undefined;
                      const isFlagged = flaggedQuestions.has(idx);
                      const isCurrent = idx === currentQIndex;

                      let bgClass = 'bg-white border-slate-200 text-slate-700';
                      if (isFlagged) {
                        bgClass = 'bg-amber-100 border-amber-300 text-amber-900 font-bold';
                      } else if (isAnswered) {
                        bgClass = 'bg-emerald-100 border-emerald-300 text-emerald-900 font-bold';
                      }

                      return (
                        <button
                          key={idx}
                          onClick={() => setCurrentQIndex(idx)}
                          className={`h-8 rounded-lg text-xs font-mono border flex items-center justify-center transition ${bgClass} ${
                            isCurrent ? 'ring-2 ring-blue-600 font-bold' : ''
                          }`}
                        >
                          {idx + 1}
                        </button>
                      );
                    })}
                  </div>

                  <div className="space-y-1.5 pt-3 border-t border-slate-100 text-[11px] text-slate-600">
                    <div className="flex items-center gap-2">
                      <span className="w-3 h-3 rounded bg-emerald-100 border border-emerald-300"></span>
                      <span>Answered ({Object.keys(studentAnswers).length})</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="w-3 h-3 rounded bg-amber-100 border border-amber-300"></span>
                      <span>Flagged ({flaggedQuestions.size})</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="w-3 h-3 rounded bg-white border border-slate-300"></span>
                      <span>Unanswered ({examQuestions.length - Object.keys(studentAnswers).length})</span>
                    </div>
                  </div>

                  {/* Submit Exam Button (Triggers reliable in-DOM modal) */}
                  <div className="mt-5 pt-3 border-t border-slate-100">
                    <button
                      onClick={() => setShowSubmitModal(true)}
                      className="w-full py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-bold transition shadow flex items-center justify-center gap-1.5 cursor-pointer"
                    >
                      <CheckCircle2 className="w-4 h-4" />
                      <span>Submit Examination</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: RESULTS DASHBOARD                                     */}
          {/* ============================================================= */}
          {currentScreen === 'RESULTS' && lastAttempt && (
            <div className="max-w-3xl mx-auto w-full py-4 space-y-5">
              <div className="bg-white rounded-2xl p-7 border border-slate-200 shadow-sm text-center">
                <div
                  className={`w-14 h-14 rounded-full flex items-center justify-center mx-auto mb-3 ${
                    lastAttempt.percentage >= 60 ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-600'
                  }`}
                >
                  <Award className="w-8 h-8" />
                </div>

                <h3 className="text-xl font-bold text-slate-900">Official Examination Results</h3>
                <p className="text-xs text-slate-500">{lastAttempt.quizTitle}</p>

                <div className="my-5 p-5 rounded-2xl bg-slate-50 border border-slate-200 inline-block min-w-[280px]">
                  <div className="text-3xl font-extrabold text-blue-950 font-mono">
                    {lastAttempt.score} / {lastAttempt.maxScore}
                  </div>
                  <div
                    className={`text-xs font-bold mt-1.5 ${
                      lastAttempt.percentage >= 60 ? 'text-emerald-700' : 'text-red-600'
                    }`}
                  >
                    {lastAttempt.percentage}% • {lastAttempt.percentage >= 60 ? 'PASSED' : 'NEEDS REVISION'}
                  </div>
                </div>

                <div className="grid grid-cols-4 gap-2 text-xs mb-6">
                  <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                    <span className="block text-slate-500 text-[10px]">Attempted</span>
                    <span className="text-sm font-bold text-slate-800">{lastAttempt.attemptedCount}</span>
                  </div>
                  <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-200 text-emerald-900">
                    <span className="block text-emerald-700 text-[10px]">Correct Answers</span>
                    <span className="text-sm font-bold">{lastAttempt.correctCount}</span>
                  </div>
                  <div className="p-3 bg-red-50 rounded-xl border border-red-200 text-red-900">
                    <span className="block text-red-700 text-[10px]">Incorrect</span>
                    <span className="text-sm font-bold">{lastAttempt.wrongCount}</span>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-xl border border-slate-200">
                    <span className="block text-slate-500 text-[10px]">Submission Type</span>
                    <span className="text-xs font-bold text-slate-800">
                      {lastAttempt.submissionType === 'AUTOMATIC_TIMEOUT' ? 'AUTO-TIMEOUT' : 'MANUAL'}
                    </span>
                  </div>
                </div>

                <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4 border-t border-slate-100">
                  <button
                    onClick={() => navigateTo('REVISION')}
                    className="w-full sm:w-auto px-5 py-2.5 bg-teal-700 hover:bg-teal-800 text-white rounded-lg text-xs font-bold shadow flex items-center justify-center gap-2 transition"
                  >
                    <ListChecks className="w-4 h-4" />
                    <span>Question-Based Revision & Mistakes</span>
                  </button>

                  <button
                    onClick={() => navigateTo('CONCEPT_ANALYTICS')}
                    className="w-full sm:w-auto px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-bold shadow flex items-center justify-center gap-2 transition"
                  >
                    <BarChart3 className="w-4 h-4" />
                    <span>Concept-Wise Analytics</span>
                  </button>

                  <button
                    onClick={() => navigateTo('STUDENT_DASHBOARD')}
                    className="w-full sm:w-auto px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium"
                  >
                    Return to Dashboard
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: QUESTION-BASED REVISION (CORE INNOVATION)            */}
          {/* ============================================================= */}
          {currentScreen === 'REVISION' && (
            <div className="max-w-4xl mx-auto w-full space-y-4 py-2">
              <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h3 className="text-lg font-bold text-slate-900">
                    Question-Based Revision & Mistake Analysis
                  </h3>
                  <p className="text-xs text-slate-500">
                    Exact questions, student choice, correct answer, and Java concept explanation from attempt {lastAttempt?.id}
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => navigateTo('CONCEPT_ANALYTICS')}
                    className="px-3.5 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold"
                  >
                    Concept Analytics →
                  </button>
                  <button
                    onClick={() =>
                      navigateTo(currentUser?.role === 'INSTRUCTOR' ? 'INSTRUCTOR_DASHBOARD' : 'STUDENT_DASHBOARD')
                    }
                    className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium"
                  >
                    Back
                  </button>
                </div>
              </div>

              <div className="space-y-3">
                {lastAttempt?.snapshots?.map((snap, idx) => {
                  let badge = (
                    <span className="text-xs font-bold px-2.5 py-0.5 rounded bg-slate-100 text-slate-600">
                      UNANSWERED (0)
                    </span>
                  );
                  if (snap.status === 'CORRECT') {
                    badge = (
                      <span className="text-xs font-bold px-2.5 py-0.5 rounded bg-emerald-100 text-emerald-800 flex items-center gap-1">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        CORRECT (+1)
                      </span>
                    );
                  } else if (snap.status === 'INCORRECT') {
                    badge = (
                      <span className="text-xs font-bold px-2.5 py-0.5 rounded bg-red-100 text-red-800 flex items-center gap-1">
                        <XCircle className="w-3.5 h-3.5" />
                        INCORRECT (0)
                      </span>
                    );
                  } else if (snap.status === 'INVALID') {
                    badge = (
                      <span className="text-xs font-bold px-2.5 py-0.5 rounded bg-amber-100 text-amber-800 flex items-center gap-1">
                        <AlertTriangle className="w-3.5 h-3.5" />
                        INVALID (0)
                      </span>
                    );
                  }

                  return (
                    <div
                      key={idx}
                      className={`bg-white rounded-xl p-5 border shadow-sm transition ${
                        snap.status === 'INCORRECT'
                          ? 'border-red-200'
                          : snap.status === 'CORRECT'
                          ? 'border-emerald-200'
                          : snap.status === 'INVALID'
                          ? 'border-amber-200 bg-amber-50/20'
                          : 'border-slate-200'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-xs text-slate-900">Q{idx + 1}.</span>
                          <span className="text-xs font-semibold px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                            {snap.concept}
                          </span>
                          <span className="text-xs text-slate-500">({snap.courseOutcome})</span>
                        </div>
                        {badge}
                      </div>

                      <p className="text-xs font-semibold text-slate-900 mb-3">{snap.questionText}</p>

                      <div className="grid sm:grid-cols-2 gap-2 text-xs mb-3">
                        {snap.options.map((opt, oIdx) => {
                          const isStudentPick = snap.studentAnswerIndex === oIdx;
                          const isCorrect = snap.correctAnswerIndex === oIdx;

                          let optClass = 'bg-slate-50 text-slate-600 border-slate-200';
                          if (isCorrect) {
                            optClass = 'bg-emerald-50 text-emerald-900 border-emerald-300 font-semibold';
                          } else if (isStudentPick && !isCorrect) {
                            optClass = 'bg-red-50 text-red-900 border-red-300 font-semibold';
                          }

                          return (
                            <div key={oIdx} className={`p-2 rounded-lg border text-xs flex items-center justify-between ${optClass}`}>
                              <span>
                                {String.fromCharCode(65 + oIdx)}) {opt}
                              </span>
                              {isCorrect && <span className="text-[10px] text-emerald-700 font-bold">Correct Answer</span>}
                              {isStudentPick && !isCorrect && (
                                <span className="text-[10px] text-red-600 font-bold">Your Choice</span>
                              )}
                            </div>
                          );
                        })}
                      </div>

                      <div className="bg-slate-50 p-2.5 rounded-lg border border-slate-200 text-xs text-slate-700">
                        <span className="font-semibold text-slate-900">Java Concept Rationale: </span>
                        {snap.explanation}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: CONCEPT-WISE PERFORMANCE ANALYTICS (USING HASHMAP)    */}
          {/* ============================================================= */}
          {currentScreen === 'CONCEPT_ANALYTICS' && (
            <div className="max-w-4xl mx-auto w-full space-y-4 py-2">
              <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h3 className="text-lg font-bold text-slate-900">
                    Concept-Wise Performance & Weakness Analytics
                  </h3>
                  <p className="text-xs text-slate-500">
                    Aggregated using Java HashMap • Identifies topics requiring revision (&lt;60% accuracy)
                  </p>
                </div>

                <button
                  onClick={() =>
                    navigateTo(currentUser?.role === 'INSTRUCTOR' ? 'INSTRUCTOR_DASHBOARD' : 'STUDENT_DASHBOARD')
                  }
                  className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium"
                >
                  Back
                </button>
              </div>

              <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="p-3.5 bg-slate-50 border-b border-slate-200 font-semibold text-xs text-slate-700">
                  Java Concept Performance Summary
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-xs text-left">
                    <thead className="bg-slate-100 text-slate-600 text-[11px] uppercase tracking-wider">
                      <tr>
                        <th className="p-3">Java Concept</th>
                        <th className="p-3">Total Seen</th>
                        <th className="p-3">Attempted</th>
                        <th className="p-3">Correct</th>
                        <th className="p-3">Accuracy</th>
                        <th className="p-3">Status</th>
                        <th className="p-3">Recommendation</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {computeConceptAnalytics().map((row, idx) => (
                        <tr key={idx} className="hover:bg-slate-50 transition">
                          <td className="p-3 font-semibold text-slate-900">{row.concept}</td>
                          <td className="p-3 text-slate-600">{row.total}</td>
                          <td className="p-3 text-slate-600">{row.attempted}</td>
                          <td className="p-3 text-emerald-600 font-semibold">{row.correct}</td>
                          <td className="p-3 font-mono font-bold text-slate-800">{row.accuracy}%</td>
                          <td className="p-3">
                            {row.statusLevel === 'WEAK' ? (
                              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-red-100 text-red-800">
                                WEAK
                              </span>
                            ) : row.statusLevel === 'MODERATE' ? (
                              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800">
                                MODERATE
                              </span>
                            ) : row.statusLevel === 'STRONG' ? (
                              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                                STRONG
                              </span>
                            ) : (
                              <span className="px-2 py-0.5 rounded text-[10px] bg-slate-100 text-slate-600">
                                NOT ATTEMPTED
                              </span>
                            )}
                          </td>
                          <td className="p-3 font-semibold">
                            <span
                              className={
                                row.recommendation === 'Needs revision'
                                  ? 'text-red-600'
                                  : row.recommendation === 'Practice more'
                                  ? 'text-amber-600'
                                  : 'text-emerald-600'
                              }
                            >
                              {row.recommendation}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: EXAMINATION HISTORY                                   */}
          {/* ============================================================= */}
          {currentScreen === 'HISTORY' && (
            <div className="max-w-4xl mx-auto w-full space-y-4 py-2">
              <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-bold text-slate-900">Examination History</h3>
                  <p className="text-xs text-slate-500">
                    {currentUser?.role === 'INSTRUCTOR' ? 'All student submissions' : 'Your completed attempts'}
                  </p>
                </div>
                <button
                  onClick={() =>
                    navigateTo(currentUser?.role === 'INSTRUCTOR' ? 'INSTRUCTOR_DASHBOARD' : 'STUDENT_DASHBOARD')
                  }
                  className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium"
                >
                  Back
                </button>
              </div>

              <div className="space-y-3">
                {attempts
                  .filter((a) => currentUser?.role === 'INSTRUCTOR' || a.studentUsername === currentUser?.username)
                  .map((att) => (
                    <div
                      key={att.id}
                      className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                    >
                      <div>
                        <div className="flex items-center gap-2 mb-1">
                          <span className="font-mono text-xs font-bold text-blue-900 bg-blue-50 px-2 py-0.5 rounded border border-blue-200">
                            {att.id}
                          </span>
                          <span className="text-xs font-bold text-slate-800">{att.quizTitle}</span>
                        </div>
                        <div className="text-xs text-slate-500 space-x-3">
                          <span>Candidate: <strong>{att.studentFullName}</strong> ({att.studentUsername})</span>
                          <span>Date: {new Date(att.submissionTime).toLocaleDateString()}</span>
                          <span
                            className={`font-semibold ${
                              att.submissionType === 'AUTOMATIC_TIMEOUT' ? 'text-red-600' : 'text-slate-600'
                            }`}
                          >
                            Type: {att.submissionType}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-4">
                        <div className="text-right">
                          <div className="text-base font-bold text-blue-950 font-mono">
                            {att.score} / {att.maxScore}
                          </div>
                          <div
                            className={`text-xs font-semibold ${
                              att.percentage >= 60 ? 'text-emerald-600' : 'text-red-600'
                            }`}
                          >
                            {att.percentage}%
                          </div>
                        </div>

                        <button
                          onClick={() => {
                            setLastAttempt(att);
                            navigateTo('REVISION');
                          }}
                          className="px-3.5 py-2 bg-teal-700 hover:bg-teal-800 text-white rounded-lg text-xs font-semibold shadow transition"
                        >
                          Review Snapshots →
                        </button>
                      </div>
                    </div>
                  ))}
              </div>
            </div>
          )}

          {/* ============================================================= */}
          {/* SCREEN: ABOUT & PROPOSAL DETAILS                              */}
          {/* ============================================================= */}
          {currentScreen === 'ABOUT' && (
            <div className="max-w-3xl mx-auto w-full py-4 space-y-4">
              <div className="bg-white rounded-2xl p-7 border border-slate-200 shadow-sm space-y-5">
                <div className="text-center border-b border-slate-200 pb-4">
                  <h3 className="text-xl font-bold text-blue-950">ALLIANCE UNIVERSITY</h3>
                  <h4 className="text-sm font-semibold text-slate-700">Alliance School of Advanced Computing</h4>
                  <p className="text-xs text-slate-500 mt-1">
                    Micro-Project: Online Quiz Management System with Synchronized Timer & Revision
                  </p>
                </div>

                <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-800 mb-2">
                    Student Team Members
                  </h4>
                  <div className="grid grid-cols-2 gap-2 text-xs font-medium text-slate-700">
                    <div>• Monisha R</div>
                    <div>• D Thanushree</div>
                    <div>• K Dheeraj Chowdary</div>
                    <div>• Harsha Vardhan E</div>
                    <div>• Kadalinga</div>
                  </div>
                </div>

                <div className="space-y-2 text-xs text-slate-700 leading-relaxed">
                  <h4 className="font-bold text-slate-900">Project Innovation:</h4>
                  <p>
                    Rather than displaying only a numeric score at exam completion, the system preserves all exact questions from the attempt along with the candidate&apos;s answer, the correct answer, and the Java concept.
                  </p>
                  <p>
                    A multithreaded synchronized timer ensures automatic submission when deadlines expire, and HashMap aggregation computes concept-wise accuracy to identify topics requiring revision.
                  </p>
                </div>

                <div className="p-3.5 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-950 space-y-1">
                  <div className="font-bold">Course Outcomes Addressed:</div>
                  <div>• <strong>CO-1:</strong> Evolve Java programs incorporating classes, objects, and inheritance.</div>
                  <div>• <strong>CO-2:</strong> Develop Java programs incorporating polymorphism, interfaces, and packages.</div>
                  <div>• <strong>CO-3:</strong> Create Java programs incorporating exception handling and multithreading.</div>
                </div>

                <div className="pt-2 flex justify-end">
                  <button
                    onClick={() => navigateTo('WELCOME')}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg text-xs font-semibold hover:bg-blue-700 transition"
                  >
                    Return to Home
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Clean Application Footer */}
        <footer className="mt-6 pt-3 border-t border-slate-200 text-center text-xs text-slate-500">
          Alliance University • School of Advanced Computing
        </footer>
      </main>

      {/* =============================================================== */}
      {/* IN-DOM SUBMISSION CONFIRMATION MODAL (NEVER BLOCKED IN IFRAME)   */}
      {/* =============================================================== */}
      {showSubmitModal && (
        <div className="fixed inset-0 bg-slate-950/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-200 animate-in fade-in duration-200">
            <div className="w-12 h-12 rounded-full bg-blue-50 text-blue-700 flex items-center justify-center mx-auto mb-4 border border-blue-100">
              <CheckCircle2 className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-bold text-slate-900 text-center">
              Confirm Examination Submission
            </h3>

            <p className="text-xs text-slate-600 text-center mt-2 leading-relaxed">
              Are you sure you want to finalize and submit your examination? Once submitted, your answers cannot be changed.
            </p>

            <div className="my-5 p-3.5 bg-slate-50 rounded-xl border border-slate-200 flex justify-around text-xs">
              <div className="text-center">
                <span className="block text-slate-400 text-[10px] uppercase font-bold">Answered</span>
                <span className="text-base font-bold text-emerald-700">
                  {Object.keys(studentAnswers).length}
                </span>
              </div>
              <div className="h-8 w-px bg-slate-200"></div>
              <div className="text-center">
                <span className="block text-slate-400 text-[10px] uppercase font-bold">Unattempted</span>
                <span className="text-base font-bold text-amber-700">
                  {examQuestions.length - Object.keys(studentAnswers).length}
                </span>
              </div>
              <div className="h-8 w-px bg-slate-200"></div>
              <div className="text-center">
                <span className="block text-slate-400 text-[10px] uppercase font-bold">Total Questions</span>
                <span className="text-base font-bold text-slate-800">
                  {examQuestions.length}
                </span>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setShowSubmitModal(false)}
                className="flex-1 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition"
              >
                Continue Answering
              </button>
              <button
                onClick={() => finalizeSubmission('MANUAL')}
                className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-bold shadow-md transition"
              >
                Confirm & Submit
              </button>
            </div>
          </div>
        </div>
      )}

      {/* =============================================================== */}
      {/* IN-DOM DELETE QUESTION CONFIRMATION MODAL                        */}
      {/* =============================================================== */}
      {questionToDelete && (
        <div className="fixed inset-0 bg-slate-950/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl border border-slate-200">
            <div className="w-10 h-10 rounded-full bg-red-50 text-red-600 flex items-center justify-center mx-auto mb-3">
              <Trash2 className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold text-slate-900 text-center">
              Delete Question {questionToDelete}?
            </h3>
            <p className="text-xs text-slate-500 text-center mt-1">
              This action will remove the question from the active bank.
            </p>
            <div className="mt-5 flex gap-2">
              <button
                onClick={() => setQuestionToDelete(null)}
                className="flex-1 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  setQuestions(questions.filter((q) => q.id !== questionToDelete));
                  showNotification(`Question ${questionToDelete} deleted.`);
                  setQuestionToDelete(null);
                }}
                className="flex-1 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-bold"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
