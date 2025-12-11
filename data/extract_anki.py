import sqlite3
import json

conn = sqlite3.connect(r'C:\my-management\backend\data\anki_temp\collection.anki2')

# Get all notes
notes = conn.execute('SELECT id, flds, tags FROM notes').fetchall()
print(f"Total notes: {len(notes)}")

# Extract data - Anki separates fields with \x1f
items = []
for note_id, flds, tags in notes:
    fields = flds.split('\x1f')
    if len(fields) >= 2:
        # Typical format: front, reading, meaning (or front, back)
        front = fields[0] if len(fields) > 0 else ""
        reading = fields[1] if len(fields) > 2 else ""
        meaning = fields[2] if len(fields) > 2 else fields[1] if len(fields) > 1 else ""
        
        # Extract lesson from tags
        lesson = "Lesson 1"
        if tags:
            for tag in tags.split():
                if "lesson" in tag.lower() or tag.startswith("L"):
                    lesson = tag.replace("_", " ")
                    break
        
        items.append({
            "front": front[:500],  # Limit length
            "reading": reading[:200] if reading else "",
            "back": meaning[:500] if meaning else "",
            "topic": lesson
        })

# Output as JSON for the API
output = {
    "courseName": "Minna no Nihongo 1 & 2",
    "description": "Lessons 1-50 vocabulary and expressions",
    "items": items
}

with open(r'C:\my-management\backend\data\anki_import.json', 'w', encoding='utf-8') as f:
    json.dump(output, f, ensure_ascii=False, indent=2)

print(f"Exported {len(items)} items to anki_import.json")
print("Sample items:")
for item in items[:3]:
    print(f"  {item['front']} | {item['reading']} | {item['back']} | {item['topic']}")
