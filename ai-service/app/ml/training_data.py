"""
Small, hand-written synthetic dataset standing in for real historical reports.
Good enough to demonstrate a working TF-IDF + Logistic Regression pipeline end to end;
swap this module out for a loader over real, labeled ReliefGrid reports once they exist —
nothing else in classifier.py needs to change.
"""

# (description, need_type, urgency)
EXAMPLES: list[tuple[str, str, str]] = [
    # RESCUE / CRITICAL
    ("Six people trapped on a rooftop, water still rising fast", "RESCUE", "CRITICAL"),
    ("Family stuck in their car, floodwater at the windows, need immediate rescue", "RESCUE", "CRITICAL"),
    ("Elderly man collapsed, can't breathe, trapped in a flooded house", "RESCUE", "CRITICAL"),
    ("Landslide has blocked the only road out, people are trapped inside", "RESCUE", "CRITICAL"),
    ("Boat capsized near the riverbank, several people in the water now", "RESCUE", "CRITICAL"),
    ("Child stranded on a tree branch surrounded by rising floodwater", "RESCUE", "CRITICAL"),
    # MEDICINE / CRITICAL or HIGH
    ("Dialysis patient cut off by flooded road, urgently needs treatment", "MEDICINE", "CRITICAL"),
    ("Diabetic child has run out of insulin, needs it immediately", "MEDICINE", "CRITICAL"),
    ("Elderly woman having chest pains, no access to her heart medication", "MEDICINE", "CRITICAL"),
    ("Insulin and blood pressure tablets needed for elderly residents at the shelter", "MEDICINE", "HIGH"),
    ("Running low on antibiotics for infected wounds at the clinic", "MEDICINE", "HIGH"),
    ("Need paracetamol and basic first aid supplies for minor injuries", "MEDICINE", "MEDIUM"),
    # FOOD
    ("Thirty families in the school hall have had nothing to eat since yesterday", "FOOD", "HIGH"),
    ("Dry rations needed for families with no cooking facilities", "FOOD", "HIGH"),
    ("Baby food and milk powder needed for infants at the shelter", "FOOD", "MEDIUM"),
    ("Running low on rice and lentils for the community kitchen", "FOOD", "MEDIUM"),
    ("Could use some snacks and extra food for the volunteers next week", "FOOD", "LOW"),
    # WATER
    ("Wells contaminated by floodwater, urgent need for clean drinking water", "WATER", "HIGH"),
    ("No safe drinking water for two days, well water looks contaminated", "WATER", "HIGH"),
    ("Water purification tablets needed for a village of two hundred people", "WATER", "MEDIUM"),
    ("Could use a few more water jugs for the shelter, not urgent", "WATER", "LOW"),
    # SHELTER
    ("Twenty five families sleeping outdoors, need tarpaulins and mats tonight", "SHELTER", "HIGH"),
    ("Roof collapsed on our house, need emergency shelter for the night", "SHELTER", "HIGH"),
    ("Tents needed for displaced families before the rain returns", "SHELTER", "MEDIUM"),
    ("Would like some extra blankets for the shelter, nothing urgent", "SHELTER", "LOW"),
    # OTHER / LOW
    ("Looking for volunteers to help clean up debris next weekend", "OTHER", "LOW"),
    ("Need a generator for the community center, not an emergency", "OTHER", "LOW"),
    ("Requesting batteries and flashlights for the neighborhood watch", "OTHER", "MEDIUM"),
    ("Fuel needed for the water pump, can wait a day or two", "OTHER", "MEDIUM"),
]

DESCRIPTIONS = [e[0] for e in EXAMPLES]
NEED_LABELS = [e[1] for e in EXAMPLES]
URGENCY_LABELS = [e[2] for e in EXAMPLES]
