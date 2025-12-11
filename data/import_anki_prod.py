import sqlite3
import json
import requests
import time

# Use raw string for Windows path or forward slashes
conn = sqlite3.connect(r'C:\my-management\backend\data\anki_temp\collection.anki2')

# Get all notes with fields: Expression, Meaning, Reading, Lesson Number
notes = conn.execute('SELECT id, flds, tags FROM notes').fetchall()
print(f"Total notes: {len(notes)}")

# Group items by lesson
course1_items = []  # Lessons 1-25
course2_items = []  # Lessons 26-50

for note_id, flds, tags in notes:
    fields = flds.split('\x1f')
    
    # Fields: Expression[0], Meaning[1], Reading[2], Lesson Number[3]
    expression = fields[0].strip() if len(fields) > 0 else ""
    meaning = fields[1].strip() if len(fields) > 1 else ""
    reading = fields[2].strip() if len(fields) > 2 else ""
    lesson_num_str = fields[3].strip() if len(fields) > 3 else "1"
    
    # Parse lesson number
    try:
        lesson_num = int(lesson_num_str)
    except:
        lesson_num = 1
    
    item = {
        "front": expression,
        "reading": reading,
        "back": meaning,
        "topic": f"Lesson {lesson_num:02d}"  # Pad for proper sorting
    }
    
    if lesson_num <= 25:
        course1_items.append(item)
    else:
        course2_items.append(item)

print(f"Course 1 (Lessons 1-25): {len(course1_items)} items")
print(f"Course 2 (Lessons 26-50): {len(course2_items)} items")

BASE_URL = 'https://japanese-study-api.onrender.com'
print(f"Connecting to {BASE_URL}...")

try:
    # Login to get token
    login_resp = requests.post(f'{BASE_URL}/api/auth/login', 
        json={"username": "admin", "password": "admin"},
        headers={"Content-Type": "application/json"})
    
    if login_resp.status_code != 200:
        print(f"Login failed: {login_resp.status_code} - {login_resp.text}")
        exit(1)

    token = login_resp.json().get('accessToken')
    print(f"Got token")

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}"
    }

    # First, delete old courses (if any)
    print("Deleting old courses...")
    try:
        courses = requests.get(f'{BASE_URL}/api/admin/courses', headers=headers).json()
        for course in courses:
            requests.delete(f"{BASE_URL}/api/admin/courses/{course['id']}", headers=headers)
            print(f"  Deleted: {course['title']}")
    except Exception as e:
        print(f"Error listing/deleting courses (might be empty): {e}")

    # Import Course 1
    print("Importing Course 1...")
    resp1 = requests.post(f'{BASE_URL}/api/admin/anki/import',
        json={
            "courseName": "Minna no Nihongo 1",
            "description": "Lessons 1-25 vocabulary and expressions",
            "items": course1_items
        },
        headers=headers)
    print(f"  Status: {resp1.status_code}")

    # Import Course 2
    print("Importing Course 2...")
    resp2 = requests.post(f'{BASE_URL}/api/admin/anki/import',
        json={
            "courseName": "Minna no Nihongo 2",
            "description": "Lessons 26-50 vocabulary and expressions",
            "items": course2_items
        },
        headers=headers)
    print(f"  Status: {resp2.status_code}")

    print("Done!")

except Exception as e:
    print(f"Migration failed: {e}")
