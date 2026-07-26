import re

with open('app/src/main/java/com/example/data/StatusRepository.kt', 'r') as f:
    content = f.read()

# 1. Remove dynamicDemoStatuses and getDemoStatuses and addNewDemoStatus
content = re.sub(r'    private val dynamicDemoStatuses.*?(?=    // 4\. Helper to save)', '', content, flags=re.DOTALL)

# 2. Remove the if (status.id.startsWith("demo_")) block from saveStatusToGallery
demo_block_pattern = r'\} else if \(status\.id\.startsWith\("demo_"\)\) \{.*?\} else \{'
content = re.sub(demo_block_pattern, '} else {', content, flags=re.DOTALL)

# 3. Remove createDefaultDemoBitmap
demo_bitmap_pattern = r'    private fun createDefaultDemoBitmap.*?return bitmap\n    \}'
content = re.sub(demo_bitmap_pattern, '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/StatusRepository.kt', 'w') as f:
    f.write(content)

