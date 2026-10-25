import type { MaterialType } from '../types'

/** Bordered colored tag classes for each material type -- see index.css .badge-type-*. */
const BADGE_CLASS: Record<MaterialType, string> = {
  NOTE: 'badge-type-note',
  TEXTBOOK: 'badge-type-textbook',
  QUESTION_PAPER: 'badge-type-question_paper',
  PRESENTATION: 'badge-type-presentation',
  OTHER: 'badge-type-other',
}

export function materialTypeBadgeClass(type: MaterialType): string {
  return `badge ${BADGE_CLASS[type]}`
}
