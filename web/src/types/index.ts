export type GlobalRole = 'USER' | 'ADMIN'
export type AccountStatus = 'ACTIVE' | 'DISABLED'
export type CommunityType = 'PUBLIC' | 'PRIVATE'
export type CommunityStatus = 'ACTIVE' | 'ARCHIVED'
export type MemberRole = 'OWNER' | 'MODERATOR' | 'MEMBER'
export type MaterialType = 'NOTE' | 'TEXTBOOK' | 'QUESTION_PAPER' | 'PRESENTATION' | 'OTHER'
export type MaterialVisibility = 'COMMUNITY' | 'PRIVATE'
export type ReportStatus = 'PENDING' | 'REVIEWED' | 'DISMISSED'
export type SortOrder = 'newest' | 'popular'

export interface User {
  userId: number
  username: string
  email: string
  displayName: string
  globalRole: GlobalRole
  accountStatus: AccountStatus
  createdAt: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface Community {
  communityId: number
  name: string
  description: string | null
  communityType: CommunityType
  status: CommunityStatus
  joinCode: string | null
  ownerId: number
  ownerUsername: string
  hasPassword: boolean
  memberCount: number
  currentUserRole: MemberRole | null
  createdAt: string
}

export interface CommunityMember {
  userId: number
  username: string
  displayName: string
  memberRole: MemberRole
  joinedAt: string
}

export interface Subject {
  subjectId: number
  communityId: number
  subjectCode: string
  subjectName: string
  createdAt: string
}

export interface Material {
  materialId: number
  communityId: number | null
  subjectId: number | null
  subjectCode: string | null
  subjectName: string | null
  uploadedByUserId: number
  uploadedByUsername: string
  title: string
  description: string | null
  materialType: MaterialType
  bookName: string | null
  authorName: string | null
  visibility: MaterialVisibility
  viewCount: number
  favorited: boolean
  latestVersionNumber: number
  createdAt: string
  updatedAt: string
}

export interface MaterialVersion {
  versionId: number
  materialId: number
  versionNumber: number
  originalFileName: string
  mimeType: string | null
  fileSizeBytes: number
  checksumSha256: string | null
  uploadedByUserId: number
  uploadedByUsername: string
  versionNotes: string | null
  createdAt: string
}

export interface QuestionPaper {
  questionPaperId: number
  examYear: number | null
  examType: string | null
  semester: number | null
  material: Material
}

export interface Report {
  reportId: number
  materialId: number
  materialTitle: string
  reportedByUserId: number
  reportedByUsername: string
  reason: string
  status: ReportStatus
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ApiErrorBody {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors: string[] | null
}
