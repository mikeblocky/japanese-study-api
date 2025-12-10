package com.japanesestudy.app.util;

import com.japanesestudy.app.model.*;
import com.japanesestudy.app.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ItemTypeRepository typeRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository itemRepository;
    private final com.japanesestudy.app.service.AnkiImportService importService;

    public DataSeeder(UserRepository userRepository,
            CourseRepository courseRepository,
            ItemTypeRepository typeRepository,
            TopicRepository topicRepository,
            StudyItemRepository itemRepository,
            com.japanesestudy.app.service.AnkiImportService importService) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.typeRepository = typeRepository;
        this.topicRepository = topicRepository;
        this.itemRepository = itemRepository;
        this.importService = importService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Always ensure users exist
        ensureUsersExist();

        // Check if we have a significant amount of data (indicating Anki import
        // happened)
        if (itemRepository.count() > 200) {
            return; // Already fully seeded
        }

        System.out.println("Seeding content...");

        // Only seed hardcoded if completely empty
        if (itemRepository.count() == 0) {
            seedHardcodedContent();
        }

        System.out.println("Attempting Anki import...");
        importService.importAnkiPackage("data/Japanese_Minna_no_Nihongo_1__2_Lessons_1_-_50.apkg");
    }

    private void seedHardcodedContent() {
        System.out.println("Seeding basic content...");

        // 2. Create Types
        ItemType wordType = new ItemType();
        wordType.setName("VOCABULARY");
        typeRepository.save(wordType);

        ItemType kanjiType = new ItemType();
        kanjiType.setName("KANJI");
        typeRepository.save(kanjiType);

        ItemType grammarType = new ItemType();
        grammarType.setName("GRAMMAR");
        typeRepository.save(grammarType);

        // 3. Create Courses
        Course n5Course = new Course();
        n5Course.setTitle("Minna no Nihongo I");
        n5Course.setDescription("Elementary Japanese (JLPT N5 Level). Chapters 1-25.");
        courseRepository.save(n5Course);

        Course n4Course = new Course();
        n4Course.setTitle("Minna no Nihongo II");
        n4Course.setDescription("Lower Intermediate Japanese (JLPT N4 Level). Chapters 26-50.");
        courseRepository.save(n4Course);

        // 4. Create Topics (Chapters)
        createTopics(n5Course, 1, 25);
        createTopics(n4Course, 26, 50);

        // 5. Populate Chapter 1 Content (Full)
        Topic ch1 = findTopic("Lesson 1", "Minna no Nihongo I");
        if (ch1 != null) {
            // Pronouns & People
            createItem(ch1, wordType, "わたし", "Watashi", "I");
            createItem(ch1, wordType, "わたしたち", "Watashitachi", "We");
            createItem(ch1, wordType, "あなた", "Anata", "You");
            createItem(ch1, wordType, "あのひと", "Anohito", "That person");
            createItem(ch1, wordType, "あのかた", "Anokata", "That person (polite)");
            createItem(ch1, wordType, "みなさん", "Minasan", "Ladies and Gentlemen, all of you");
            createItem(ch1, wordType, "せんせい", "Sensei", "Teacher, Instructor (not used for self)");
            createItem(ch1, wordType, "きょうし", "Kyoushi", "Teacher, Instructor (used for self)");
            createItem(ch1, wordType, "がくせい", "Gakusei", "Student");
            createItem(ch1, wordType, "かいしゃいん", "Kaishain", "Company employee");
            createItem(ch1, wordType, "しゃいん", "Shain", "Employee of ~ Company (used with company name)");
            createItem(ch1, wordType, "ぎんこういん", "Ginkouin", "Bank employee");
            createItem(ch1, wordType, "いしゃ", "Isha", "Doctor");
            createItem(ch1, wordType, "けんきゅうしゃ", "Kenkyuusha", "Researcher, Scholar");
            createItem(ch1, wordType, "エンジニア", "Enjinia", "Engineer");
            createItem(ch1, wordType, "だいがく", "Daigaku", "University");
            createItem(ch1, wordType, "びょういん", "Byouin", "Hospital");
            createItem(ch1, wordType, "でんき", "Denki", "Electricity, Light");
            createItem(ch1, wordType, "だれ", "Dare", "Who");
            createItem(ch1, wordType, "どなた", "Donata", "Who (polite)");
            createItem(ch1, wordType, "—さい", "—sai", "—years old");
            createItem(ch1, wordType, "なんさい", "Nansai", "How old");
            createItem(ch1, wordType, "はい", "Hai", "Yes");
            createItem(ch1, wordType, "いいえ", "Iie", "No");

            // Grammar
            createItem(ch1, grammarType, "N1 は N2 です", "N1 wa N2 desu", "N1 is N2 (Topic Marker)");
            createItem(ch1, grammarType, "N1 は N2 じゃありません", "N1 wa N2 ja arimasen", "N1 is not N2");
            createItem(ch1, grammarType, "N1 は N2 ですか", "N1 wa N2 desu ka", "Is N1 N2? (Question)");
            createItem(ch1, grammarType, "Nも", "N mo", "N also / N too (Particle)");
        }

        // 6. Populate Chapter 2 Content (Full)
        Topic ch2 = findTopic("Lesson 2", "Minna no Nihongo I");
        if (ch2 != null) {
            // Objects & Demonstratives
            createItem(ch2, wordType, "これ", "Kore", "This (thing here)");
            createItem(ch2, wordType, "それ", "Sore", "That (thing near you)");
            createItem(ch2, wordType, "あれ", "Are", "That (thing over there)");
            createItem(ch2, wordType, "この〜", "Kono", "This ~ (modifying noun)");
            createItem(ch2, wordType, "その〜", "Sono", "That ~ (modifying noun)");
            createItem(ch2, wordType, "あの〜", "Ano", "That ~ (modifying noun over there)");
            createItem(ch2, wordType, "ほん", "Hon", "Book");
            createItem(ch2, wordType, "じしょ", "Jisho", "Dictionary");
            createItem(ch2, wordType, "ざっし", "Zasshi", "Magazine");
            createItem(ch2, wordType, "しんぶん", "Shinbun", "Newspaper");
            createItem(ch2, wordType, "ノート", "Nooto", "Notebook");
            createItem(ch2, wordType, "てちょう", "Techou", "Pocket notebook");
            createItem(ch2, wordType, "めいし", "Meishi", "Business card");
            createItem(ch2, wordType, "カード", "Kaado", "Card");
            createItem(ch2, wordType, "えんぴつ", "Enpitsu", "Pencil");
            createItem(ch2, wordType, "ボールペン", "Boorupen", "Ballpoint pen");
            createItem(ch2, wordType, "かぎ", "Kagi", "Key");
            createItem(ch2, wordType, "とけい", "Tokei", "Watch, Clock");
            createItem(ch2, wordType, "かさ", "Kasa", "Umbrella");
            createItem(ch2, wordType, "かばん", "Kaban", "Bag, Briefcase");
            createItem(ch2, wordType, "くるま", "Kuruma", "Car, Vehicle");
            createItem(ch2, wordType, "つくえ", "Tsukue", "Desk");
            createItem(ch2, wordType, "いす", "Isu", "Chair");
            createItem(ch2, wordType, "チョコレート", "Chokoreeto", "Chocolate");
            createItem(ch2, wordType, "コーヒー", "Koohii", "Coffee");
            createItem(ch2, wordType, "えいご", "Eigo", "English language");
            createItem(ch2, wordType, "にほんご", "Nihongo", "Japanese language");
            createItem(ch2, wordType, "なん", "Nan", "What");
            createItem(ch2, wordType, "そう", "Sou", "So");

            // Grammar
            createItem(ch2, grammarType, "これ は N です", "Kore wa N desu", "This is a N");
            createItem(ch2, grammarType, "それ は N ですか", "Sore wa N desu ka", "Is that a N?");
            createItem(ch2, grammarType, "これ は なん ですか", "Kore wa nan desu ka", "What is this?");
            createItem(ch2, grammarType, "N1 の N2", "N1 no N2", "N1's N2 (Possession/Modification)");
        }

        System.out.println("Data seeding completed with True Lessons content!");

        // 7. Import Anki Package if available
        // End of hardcoded seeding
    }

    private Topic findTopic(String topicTitle, String courseTitle) {
        return topicRepository.findAll().stream()
                .filter(t -> t.getTitle().contains(topicTitle) && t.getCourse().getTitle().contains(courseTitle))
                .findFirst()
                .orElse(null);
    }

    private void createTopics(Course course, int start, int end) {
        List<Topic> topics = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            Topic t = new Topic();
            t.setTitle("Lesson " + i);
            t.setDescription("Vocabulary and Grammar for Lesson " + i);
            t.setCourse(course);
            t.setOrderIndex(i);
            topics.add(t);
        }
        topicRepository.saveAll(topics);
    }

    private void createItem(Topic topic, ItemType type, String primary, String secondary, String meaning) {
        StudyItem item = new StudyItem();
        item.setPrimaryText(primary);
        item.setSecondaryText(secondary);
        item.setMeaning(meaning);
        item.setTopic(topic);
        item.setType(type);
        itemRepository.save(item);
    }

    private void ensureUsersExist() {
        // Check and create student1
        if (userRepository.findByUsername("student1").isEmpty()) {
            User user = new User();
            user.setUsername("student1");
            user.setPassword("password");
            user.setEmail("student@example.com");
            user.setRole("STUDENT");
            userRepository.save(user);
            System.out.println("Created user: student1");
        }

        // Check and create manager1
        if (userRepository.findByUsername("manager1").isEmpty()) {
            User manager = new User();
            manager.setUsername("manager1");
            manager.setPassword("password");
            manager.setEmail("manager@example.com");
            manager.setRole("MANAGER");
            userRepository.save(manager);
            System.out.println("Created user: manager1");
        }

        // Check and create admin1
        if (userRepository.findByUsername("admin1").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin1");
            admin.setPassword("password");
            admin.setEmail("admin@example.com");
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("Created user: admin1");
        }
    }
}
