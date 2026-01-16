import sqlite3
import json
import requests
import sys

# Force utf-8 output
sys.stdout.reconfigure(encoding='utf-8')

try:
    conn = sqlite3.connect(r'C:\my-management\backend\data\anki_temp\collection.anki2')
    
    # Get collection to parse models/field names
    col = conn.execute('SELECT models FROM col').fetchone()[0]
    models = json.loads(col)
    print(f"Models loaded. Count: {len(models)}")
    
    # Get all notes
    notes = conn.execute('SELECT id, mid, flds, tags FROM notes').fetchall()
    print(f"Total notes: {len(notes)}")
    
    # Group items by lesson
    course1_items = []  # Lessons 1-25
    course2_items = []  # Lessons 26-50
    
    for i, (note_id, mid, flds, tags) in enumerate(notes):
        try:
            model = models.get(str(mid))
            if not model:
                if i < 5: print(f"Model not found for mid: {mid}")
                continue
            
            # Debug first item
            if i == 0:
                print(f"First Model type: {type(model)}")
                if isinstance(model, dict):
                     flds_def = model.get('flds')
                     print(f"First Model flds type: {type(flds_def)}")
                     if isinstance(flds_def, list) and len(flds_def) > 0:
                         print(f"First field type: {type(flds_def[0])}")
                         print(f"First field content: {flds_def[0]}")
            
            if not isinstance(model, dict):
                print(f"Skipping mid {mid}, model is not dict: {type(model)}")
                continue

            # Extract field names safely
            field_names = []
            for f in model['flds']:
                if isinstance(f, dict):
                    field_names.append(f['name'])
                else:
                     print(f"Warning: Unexpected field def type: {type(f)}")
            
            fields_list = flds.split('\x1f')
            
            # Create dynamic map
            fields_map = {}
            for j, name in enumerate(field_names):
                if j < len(fields_list):
                    fields_map[name] = fields_list[j]
                    
            # Try to find lesson number
            lesson_num = 1
            for name, value in fields_map.items():
                if "Lesson" in name or "Chapter" in name:
                    try:
                        import re
                        # Extract first number found
                        m = re.search(r'\d+', value)
                        if m:
                            lesson_num = int(m.group())
                            break
                    except:
                        pass
        
            item = {
                "fields": fields_map,
                "topic": f"Lesson {lesson_num:02d}"
            }
            
            if lesson_num <= 25:
                course1_items.append(item)
            else:
                course2_items.append(item)
                
        except Exception as e:
            print(f"Error processing note {note_id}: {e}")
            continue

    print(f"Course 1 items: {len(course1_items)}")
    print(f"Course 2 items: {len(course2_items)}")
    
    # Login to get token
    try:
        login_resp = requests.post('http://localhost:8080/api/auth/login', 
            json={"username": "admin", "password": "admin"},
            headers={"Content-Type": "application/json"})
        
        if login_resp.status_code != 200:
             # Try clean credentials
             login_resp = requests.post('http://localhost:8080/api/auth/login', 
                json={"username": "sa", "password": "password"},
                headers={"Content-Type": "application/json"})
             
        if login_resp.status_code != 200:
            print(f"Login failed: {login_resp.status_code} {login_resp.text}")
            # Try registering if admin doesn't exist (fresh DB)
            reg_resp = requests.post('http://localhost:8080/api/auth/register',
                json={"username": "admin", "password": "password"},
                headers={"Content-Type": "application/json"})
            if reg_resp.status_code == 200:
                login_resp = requests.post('http://localhost:8080/api/auth/login', 
                    json={"username": "admin", "password": "password"},
                    headers={"Content-Type": "application/json"})
        
        token = login_resp.json().get('accessToken')
        print(f"Got token: {token is not None}")
        
        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}"
        }
        
        # Import Course 1
        print("Importing Course 1...")
        resp1 = requests.post('http://localhost:8080/api/admin/anki/import',
            json={
                "courseName": "Minna no Nihongo 1",
                "description": "Lessons 1-25 vocabulary and expressions",
                "items": course1_items
            },
            headers=headers)
        if resp1.status_code != 200:
            print(f"  ERROR: {resp1.status_code}")
            print(f"  Response: {resp1.text}")
        else:
            print(f"  Status: {resp1.status_code}")
        
        # Import Course 2
        print("Importing Course 2...")
        resp2 = requests.post('http://localhost:8080/api/admin/anki/import',
            json={
                "courseName": "Minna no Nihongo 2",
                "description": "Lessons 26-50 vocabulary and expressions",
                "items": course2_items
            },
            headers=headers)
        if resp2.status_code != 200:
            print(f"  ERROR: {resp2.status_code}")
            print(f"  Response: {resp2.text}")
        else:
            print(f"  Status: {resp2.status_code}")
        
    except Exception as e:
        print(f"API Error: {e}")

except Exception as main_e:
    print(f"CRITICAL ERROR: {main_e}")
    import traceback
    traceback.print_exc()
