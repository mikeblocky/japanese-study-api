import urllib.request
import json
import sys

try:
    with urllib.request.urlopen("http://localhost:8081/api/data/courses") as url:
        data = json.loads(url.read().decode())
        
        n4_course = None
        for course in data:
            if "Minna no Nihongo II" in course["title"]:
                n4_course = course
                break
        
        if not n4_course:
            print("FAILURE: Minna no Nihongo II course not found")
            sys.exit(1)
            
        print(f"Checking N4 Course: {n4_course['title']}")
        topics = n4_course["topics"]
        for t in topics:
            count = len(t.get("studyItems", []))
            print(f"Topic: '{t['title']}' (ID: {t['id']}) - Items: {count}")
            
        # Check Lesson 1 in N5 just to see if import works generally
        n5_course = next((c for c in data if "Minna no Nihongo I" in c["title"]), None)
        if n5_course:
            l1 = next((t for t in n5_course["topics"] if "Lesson 1" in t["title"]), None)
            if l1:
                print(f"Lesson 1 (N5) Items: {len(l1.get('studyItems', []))}")


except Exception as e:
    print(f"ERROR: {e}")
    sys.exit(1)
