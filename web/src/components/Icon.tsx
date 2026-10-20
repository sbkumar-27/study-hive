import type { ReactElement, SVGProps } from 'react'

export type IconName =
  | 'communities'
  | 'materials'
  | 'vault'
  | 'papers'
  | 'reports'
  | 'user'
  | 'logout'
  | 'menu'
  | 'close'
  | 'search'
  | 'upload'
  | 'download'
  | 'star'
  | 'starFilled'
  | 'lock'
  | 'globe'
  | 'file'
  | 'empty'
  | 'warning'
  | 'check'
  | 'plus'

const paths: Record<IconName, ReactElement> = {
  communities: (
    <>
      <circle cx="9" cy="8" r="2.75" />
      <circle cx="16" cy="9" r="2.25" />
      <path d="M3.5 19c.5-3 2.6-4.75 5.5-4.75s5 1.75 5.5 4.75" />
      <path d="M14.5 14.6c2.3.3 3.8 1.9 4.2 4.4" />
    </>
  ),
  materials: (
    <>
      <path d="M5 4.5h10l4 4V19a.5.5 0 0 1-.5.5H5A.5.5 0 0 1 4.5 19V5a.5.5 0 0 1 .5-.5Z" />
      <path d="M15 4.5V9h4" />
      <path d="M8 13h8M8 16.5h8" />
    </>
  ),
  vault: (
    <>
      <rect x="4.5" y="10" width="15" height="10" rx="1.5" />
      <path d="M7.5 10V7a4.5 4.5 0 0 1 9 0v3" />
      <circle cx="12" cy="15" r="1.4" />
    </>
  ),
  papers: (
    <>
      <path d="M6.5 3.5h8l3 3V20a.5.5 0 0 1-.5.5h-11A.5.5 0 0 1 5.5 20V4a.5.5 0 0 1 1-.5Z" />
      <path d="M14.5 3.5V7h3" />
      <path d="M8.5 11h7M8.5 14h7M8.5 17h4" />
    </>
  ),
  reports: (
    <>
      <path d="M6 20V4" />
      <path d="M6 4.5h9l-1.8 3.3L15 11H6" />
    </>
  ),
  user: (
    <>
      <circle cx="12" cy="8.5" r="3.5" />
      <path d="M4.5 20c.9-4 3.6-6 7.5-6s6.6 2 7.5 6" />
    </>
  ),
  logout: (
    <>
      <path d="M9 4.5H5.5a1 1 0 0 0-1 1v13a1 1 0 0 0 1 1H9" />
      <path d="M13.5 15.5 18 11l-4.5-4.5" />
      <path d="M8.5 11H18" />
    </>
  ),
  menu: (
    <>
      <path d="M4 6.5h16M4 12h16M4 17.5h16" />
    </>
  ),
  close: (
    <>
      <path d="M5.5 5.5 18.5 18.5M18.5 5.5 5.5 18.5" />
    </>
  ),
  search: (
    <>
      <circle cx="10.5" cy="10.5" r="6" />
      <path d="M19 19l-4.3-4.3" />
    </>
  ),
  upload: (
    <>
      <path d="M12 15.5V5" />
      <path d="M7.5 9 12 4.5 16.5 9" />
      <path d="M5 16v2.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V16" />
    </>
  ),
  download: (
    <>
      <path d="M12 4.5V15" />
      <path d="M7.5 10.5 12 15l4.5-4.5" />
      <path d="M5 16v2.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V16" />
    </>
  ),
  star: <path d="M12 4.5l2.1 4.5 4.9.6-3.6 3.5.9 4.9L12 15.6l-4.3 2.4.9-4.9-3.6-3.5 4.9-.6L12 4.5Z" />,
  starFilled: (
    <path
      d="M12 4.5l2.1 4.5 4.9.6-3.6 3.5.9 4.9L12 15.6l-4.3 2.4.9-4.9-3.6-3.5 4.9-.6L12 4.5Z"
      fill="currentColor"
    />
  ),
  lock: (
    <>
      <rect x="5.5" y="10.5" width="13" height="9" rx="1.5" />
      <path d="M8.5 10.5V7.5a3.5 3.5 0 0 1 7 0v3" />
    </>
  ),
  globe: (
    <>
      <circle cx="12" cy="12" r="7.5" />
      <path d="M4.5 12h15M12 4.5c2 2.2 3 5 3 7.5s-1 5.3-3 7.5c-2-2.2-3-5-3-7.5s1-5.3 3-7.5Z" />
    </>
  ),
  file: (
    <>
      <path d="M6.5 3.5h7l4 4V20a.5.5 0 0 1-.5.5h-11a.5.5 0 0 1-.5-.5V4a.5.5 0 0 1 .5-.5Z" />
      <path d="M13.5 3.5V7.5h4" />
    </>
  ),
  empty: (
    <>
      <path d="M4.5 8.5 12 4.5l7.5 4v7L12 19.5l-7.5-4Z" />
      <path d="M4.5 8.5 12 12.5l7.5-4M12 12.5V19.5" />
    </>
  ),
  warning: (
    <>
      <path d="M12 4.5 20.5 19H3.5Z" />
      <path d="M12 10v3.5" />
      <circle cx="12" cy="16.2" r="0.4" fill="currentColor" />
    </>
  ),
  check: <path d="M5 12.5 9.5 17 19 7" />,
  plus: (
    <>
      <path d="M12 5v14M5 12h14" />
    </>
  ),
}

export function Icon({
  name,
  size = 18,
  ...rest
}: { name: IconName; size?: number } & SVGProps<SVGSVGElement>) {
  return (
    <svg
      className="icon"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.6}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...rest}
    >
      {paths[name]}
    </svg>
  )
}
