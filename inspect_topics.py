import urllib.request
import json

try:
    with urllib.request.urlopen("http://localhost:8081/api/data/courses") as url:
        data = json.loads(url.read().decode())
        
        for course in data:
            print(f"Course: {course['title']}")
            for topic in course['topics']:
                title = topic['title']
                t_id = topic['id']
                count = len(topic.get('studyItems', []))
                # Print with surrounding quotes to see whitespace
                print(f"  Topic ID {t_id}: '{title}' - Order: {topic.get('orderIndex')} - Items: {count}")
                
except Exception as e:
    print(e)
