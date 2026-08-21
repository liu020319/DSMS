const MINIMUM_HUMAN_REACTION_MS = 700

export function humanChallengeReadyAt() {
  return Date.now() + MINIMUM_HUMAN_REACTION_MS
}

export async function waitUntilHumanChallengeReady(readyAt) {
  const remaining = Number(readyAt || 0) - Date.now()
  if (remaining > 0) {
    await new Promise(resolve => window.setTimeout(resolve, remaining))
  }
}
