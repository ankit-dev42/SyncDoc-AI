import React, { useCallback, useState } from 'react';

const STORAGE_KEY = 'syncdoc:onboardingDone';

const STEPS = [
  {
    title: 'Send your first message',
    body: 'Type a message in any channel and press Enter to send. Your message is delivered in real time.',
    emoji: '💬',
  },
  {
    title: 'Set your status',
    body: 'Click the presence badge next to your name to set yourself as Online, Away, or Do Not Disturb.',
    emoji: '🟢',
  },
  {
    title: 'Search everything',
    body: 'Use the search bar to find any message across all channels. Try "from:alice" or "in:general".',
    emoji: '🔍',
  },
];

function hasCompletedOnboarding(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

function markOnboardingDone(): void {
  try {
    localStorage.setItem(STORAGE_KEY, 'true');
  } catch {
    /* noop */
  }
}

/**
 * Multi-step onboarding tour shown to first-time users.
 * Renders nothing if the user has already completed onboarding.
 */
export const OnboardingFlow: React.FC = () => {
  const [visible, setVisible] = useState(() => !hasCompletedOnboarding());
  const [step, setStep] = useState(0);

  const dismiss = useCallback(() => {
    markOnboardingDone();
    setVisible(false);
  }, []);

  const advance = useCallback(() => {
    if (step < STEPS.length - 1) {
      setStep((s) => s + 1);
    } else {
      dismiss();
    }
  }, [step, dismiss]);

  if (!visible) return null;

  const current = STEPS[step];
  const isLast = step === STEPS.length - 1;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Welcome to SyncDoc AI"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
    >
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-2xl">
        {/* Step indicator */}
        <div className="mb-5 flex items-center gap-1.5" aria-label={`Step ${step + 1} of ${STEPS.length}`}>
          {STEPS.map((_, i) => (
            <span
              key={i}
              className={`h-1.5 flex-1 rounded-full transition-colors ${
                i <= step ? 'bg-blue-500' : 'bg-slate-200'
              }`}
            />
          ))}
        </div>

        <p className="mb-2 text-4xl">{current.emoji}</p>
        <h2 className="mb-2 text-xl font-bold text-slate-900">{current.title}</h2>
        <p className="mb-6 text-sm text-slate-600">{current.body}</p>

        <div className="flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={dismiss}
            className="text-sm text-slate-400 hover:text-slate-600 focus:outline-none focus-visible:underline"
          >
            Skip tour
          </button>
          <button
            type="button"
            onClick={advance}
            className="rounded-lg bg-blue-500 px-5 py-2 text-sm font-semibold text-white
                       hover:bg-blue-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-400"
          >
            {isLast ? "Got it, let's go!" : 'Next →'}
          </button>
        </div>
      </div>
    </div>
  );
};
