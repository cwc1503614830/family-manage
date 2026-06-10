export function resolvePhotoDate(photo) {
  const raw = photo.taken_at || photo.takenAt || photo.created_at || photo.createdAt
  if (!raw) {
    return null
  }
  const text = String(raw).replace('T', ' ')
  const datePart = text.slice(0, 10)
  if (!/^\d{4}-\d{2}-\d{2}$/.test(datePart)) {
    return null
  }
  return datePart
}

export function formatDateLabel(dateKey) {
  const parts = dateKey.split('-')
  if (parts.length !== 3) {
    return dateKey
  }
  return `${parts[0]}年${parts[1]}月${parts[2]}日`
}

export function formatMilestone(referenceDate, targetDateKey) {
  if (!referenceDate || !targetDateKey) {
    return ''
  }
  const ref = new Date(String(referenceDate).slice(0, 10))
  const target = new Date(targetDateKey)
  if (Number.isNaN(ref.getTime()) || Number.isNaN(target.getTime()) || target < ref) {
    return ''
  }
  let months = (target.getFullYear() - ref.getFullYear()) * 12 + (target.getMonth() - ref.getMonth())
  if (target.getDate() < ref.getDate()) {
    months -= 1
  }
  if (months <= 0) {
    return ''
  }
  const years = Math.floor(months / 12)
  const remainMonths = months % 12
  const yearText = years > 0 ? `${years}年` : ''
  const monthText = remainMonths > 0 ? `${remainMonths}个月` : ''
  return `${yearText}${monthText}`
}

export function groupPhotosByDate(photos, options = {}) {
  const referenceDate = options.referenceDate || null
  const showMilestone = Boolean(options.showMilestone)
  const map = new Map()

  ;(photos || []).forEach(photo => {
    const dateKey = resolvePhotoDate(photo)
    if (!dateKey) {
      return
    }
    if (!map.has(dateKey)) {
      map.set(dateKey, [])
    }
    map.get(dateKey).push(photo)
  })

  return Array.from(map.entries())
    .sort((a, b) => b[0].localeCompare(a[0]))
    .map(([dateKey, items]) => ({
      dateKey,
      dateLabel: formatDateLabel(dateKey),
      milestone: showMilestone ? formatMilestone(referenceDate, dateKey) : '',
      photos: items
    }))
}
