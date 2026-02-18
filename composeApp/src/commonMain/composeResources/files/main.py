import json
from collections import Counter

def dedupe_cities(input_path: str, output_path: str = "sorted_city_list_deduped.json"):
    with open(input_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    print(f"Завантажено {len(data)} записів.")

    seen = set()
    deduped = []
    removed = 0

    for c in data:
        name = (c.get("name") or "").strip()
        oblast = (c.get("oblast") or "").strip()
        key = (name, oblast)

        if key in seen:
            removed += 1
            continue

        seen.add(key)
        deduped.append(c)

    print(f"Видалено абсолютних дублів (name+oblast): {removed}")
    print(f"Залишилось записів: {len(deduped)}")

    # (опційно) контроль: переконайся, що дублів більше нема
    counts = Counter(((x.get("name") or "").strip(), (x.get("oblast") or "").strip()) for x in deduped)
    still_dups = [(k, v) for k, v in counts.items() if v > 1]
    if still_dups:
        print("⚠️ УВАГА: після дедуплікації все ще є дублікати (неочікувано):")
        for (name, oblast), v in still_dups:
            print(f"- {name} — {oblast}: {v}")
    else:
        print("✅ Перевірка пройдена: абсолютних дублів не залишилось.")

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(deduped, f, ensure_ascii=False, indent=2)

    print(f"Записано у файл: {output_path}")
dedupe_cities("sorted_city_list.json", "sorted_city_list.json")
