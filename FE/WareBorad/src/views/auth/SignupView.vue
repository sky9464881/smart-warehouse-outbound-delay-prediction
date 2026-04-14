<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const displayName = ref('')
const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const isSubmitting = ref(false)
const localError = ref('')

const redirectTo = computed(() => {
  const value = route.query.redirect
  return typeof value === 'string' && value.length ? value : '/'
})

async function handleSubmit() {
  localError.value = ''

  if (password.value !== passwordConfirm.value) {
    localError.value = '비밀번호가 일치하지 않습니다.'
    return
  }

  isSubmitting.value = true
  try {
    await auth.signup({
      displayName: displayName.value,
      email: email.value,
      password: password.value,
    })
    await router.push(redirectTo.value)
  } catch (error) {
    localError.value = auth.errorMessage || error.message || 'Signup failed.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="auth-shell">
    <section class="auth-card" aria-label="Sign up">
      <header class="auth-head">
        <div class="brand">
          <div class="brand-mark" aria-hidden="true">WB</div>
          <div class="brand-copy">
            <strong>WareBoard</strong>
            <span>Outbound delay dashboard</span>
          </div>
        </div>
        <p class="auth-kicker">회원가입</p>
        <p class="auth-note">첫 가입자는 전체 관리자 권한이 자동으로 부여됩니다.</p>
      </header>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <label class="field">
          <span class="field-label">Name</span>
          <input v-model.trim="displayName" type="text" autocomplete="name" required />
        </label>

        <label class="field">
          <span class="field-label">Email</span>
          <input v-model.trim="email" type="email" autocomplete="email" required />
        </label>

        <label class="field">
          <span class="field-label">Password</span>
          <input v-model="password" type="password" autocomplete="new-password" minlength="8" required />
        </label>

        <label class="field">
          <span class="field-label">Confirm</span>
          <input v-model="passwordConfirm" type="password" autocomplete="new-password" minlength="8" required />
        </label>

        <p v-if="localError" class="error" role="alert">{{ localError }}</p>

        <button class="submit" type="submit" :disabled="auth.isLoading || isSubmitting">
          {{ auth.isLoading || isSubmitting ? '가입 중...' : '회원가입' }}
        </button>
      </form>

      <footer class="auth-foot">
        <span>이미 계정이 있나요?</span>
        <RouterLink class="link" :to="{ name: 'login', query: { redirect: redirectTo } }">로그인</RouterLink>
      </footer>
    </section>
  </main>
</template>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 2.2rem 1.2rem;
  background:
    radial-gradient(circle at 12% 10%, rgba(37, 99, 235, 0.12), transparent 30%),
    radial-gradient(circle at 92% 0%, rgba(249, 115, 22, 0.14), transparent 32%),
    linear-gradient(180deg, #ffffff 0%, #fffaf6 34%, #eef4ff 100%);
  color: #18212f;
  font-family: 'Aptos', 'Trebuchet MS', 'Segoe UI', sans-serif;
}

.auth-card {
  width: min(28rem, 100%);
  border-radius: 1.7rem;
  padding: 1.6rem 1.55rem 1.35rem;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(15, 23, 42, 0.1);
  box-shadow: 0 30px 70px rgba(15, 23, 42, 0.12);
}

.auth-head {
  display: grid;
  gap: 0.65rem;
  margin-bottom: 1.15rem;
}

.auth-kicker {
  color: #475569;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-size: 0.78rem;
}

.auth-note {
  color: #64748b;
  font-weight: 800;
  font-size: 0.9rem;
  line-height: 1.55;
}

.brand {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.8rem;
  align-items: center;
}

.brand-mark {
  width: 2.65rem;
  height: 2.65rem;
  border-radius: 0.95rem;
  display: grid;
  place-items: center;
  font-weight: 900;
  letter-spacing: 0.08em;
  background: rgba(15, 23, 42, 0.08);
}

.brand-copy {
  display: grid;
  gap: 0.1rem;
}

.brand-copy strong {
  font-family: 'Bahnschrift', 'Aptos Display', 'Trebuchet MS', sans-serif;
  font-size: 1.08rem;
  font-weight: 900;
  color: #0f172a;
}

.brand-copy span {
  font-size: 0.86rem;
  font-weight: 800;
  color: #64748b;
}

.auth-form {
  display: grid;
  gap: 0.9rem;
}

.field {
  display: grid;
  gap: 0.45rem;
}

.field-label {
  color: #475569;
  font-size: 0.82rem;
  font-weight: 900;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.field input {
  min-height: 2.9rem;
  padding: 0.7rem 0.85rem;
  border-radius: 1rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.95);
  color: #0f172a;
}

.field input:focus {
  outline: none;
  border-color: rgba(37, 99, 235, 0.5);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.14);
}

.error {
  color: #b91c1c;
  font-weight: 800;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 1rem;
  padding: 0.65rem 0.8rem;
  font-size: 0.9rem;
}

.submit {
  margin-top: 0.2rem;
  min-height: 3rem;
  border: 0;
  border-radius: 1.1rem;
  background: linear-gradient(135deg, #1d4ed8, #0ea5e9);
  color: #fff;
  font-weight: 900;
  letter-spacing: 0.02em;
  cursor: pointer;
  box-shadow: 0 18px 45px rgba(29, 78, 216, 0.22);
}

.submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.auth-foot {
  margin-top: 1.1rem;
  padding-top: 1.05rem;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  display: flex;
  justify-content: center;
  gap: 0.6rem;
  color: #64748b;
  font-weight: 800;
}

.link {
  color: #1d4ed8;
  font-weight: 900;
}

.link:hover {
  text-decoration: underline;
}
</style>

