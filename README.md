# Git Commit Convention

## Format

```
type(scope) : subject
```

- **No period** at the end
- **English only**

---

## Types

| type | description |
|---|---|
| `feat` | new feature |
| `fix` | bug fix |
| `docs` | documentation |
| `style` | formatting |
| `refactor` | refactoring (no feature change, performance improvement) |
| `test` | test code |
| `chore` | maintenance & misc |

---

## Subject Rules

- imperative mood (present tense)
- all lowercase
- no period at the end

---

## Examples

```
feat(api) : add order delay prediction api
fix(auth) : fix login button error
docs(db) : update warehouse schema docs
style(ui) : format dashboard layout
refactor(db) : improve warehouse query
test(api) : add robot summary endpoint test
chore : update dependencies
```