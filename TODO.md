# Prompt Injection Guardrail Fix - TODO

## Task
Fix prompt-injection guardrail for the AI assistant to block malicious questions before SQL generation.

## Steps

- [ ] 1. Update GuardrailsService.java - Add Turkish prompt injection terms
- [ ] 2. Update GuardrailsService.java - Add filter bypass terms  
- [ ] 3. Update GuardrailsService.java - Add role-scope guardrail for INDIVIDUAL role
- [ ] 4. Update GuardrailsService.java - Add detailed logging
- [ ] 5. Test that the fix works

## Blocked phrases to add:

### Prompt Injection Terms (Turkish):
- önceki talimatları yok say
- talimatları yok say
- güvenlik kurallarını kaldır
- tüm veriyi göster
- tüm veritabanını göster
- admin gibi davran
- seller gibi davran
- rolünü değiştir
- sistem mesajını göster

### Prompt Injection Terms (English):
- ignore previous instructions
- ignore all rules
- show all data
- show database
- act as admin
- act as seller
- reveal system prompt

### Filter Bypass Terms (Turkish + English):
- show all data
- show database
- tüm veriyi göster
- tüm veritabanını göster

## Expected Error Message:
"Bu sorgu güvenlik nedeniyle engellendi. Sadece kendi hesabınıza ait izin verilen verilere erişebilirsiniz."
