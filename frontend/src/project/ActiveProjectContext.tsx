import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getProjects } from '../api/projects'
import type { Project } from '../api/types'
import {
  ACTIVE_PROJECT_STORAGE_KEY,
  readStoredProjectId,
  resolveActiveProjectId,
  selectableProjects,
} from './projectSelection'

type ActiveProjectContextValue = {
  projects: Project[]
  selectable: Project[]
  activeProjectId: number | null
  activeProject: Project | null
  loading: boolean
  error: string | null
  selectProject: (projectId: number) => void
  refreshProjects: (preferredId?: number | null) => Promise<void>
}

const ActiveProjectContext = createContext<ActiveProjectContextValue | null>(null)

function persistSelection(projectId: number | null) {
  if (projectId == null) {
    localStorage.removeItem(ACTIVE_PROJECT_STORAGE_KEY)
    return
  }
  localStorage.setItem(ACTIVE_PROJECT_STORAGE_KEY, String(projectId))
}

export function ActiveProjectProvider({ children }: { children: ReactNode }) {
  const [projects, setProjects] = useState<Project[]>([])
  const [activeProjectId, setActiveProjectId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refreshProjects = useCallback(async (preferredId?: number | null) => {
    setLoading(true)
    setError(null)
    try {
      const loaded = await getProjects()
      setProjects(loaded)
      setActiveProjectId((current) => {
        const stored = preferredId !== undefined
          ? preferredId
          : current ?? readStoredProjectId(localStorage.getItem(ACTIVE_PROJECT_STORAGE_KEY))
        const resolved = resolveActiveProjectId(loaded, stored)
        persistSelection(resolved)
        return resolved
      })
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not load projects')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    getProjects(controller.signal)
      .then((loaded) => {
        if (controller.signal.aborted) {
          return
        }
        setProjects(loaded)
        const stored = readStoredProjectId(localStorage.getItem(ACTIVE_PROJECT_STORAGE_KEY))
        const resolved = resolveActiveProjectId(loaded, stored)
        persistSelection(resolved)
        setActiveProjectId(resolved)
        setError(null)
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setError(cause instanceof Error ? cause.message : 'Could not load projects')
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      })
    return () => controller.abort()
  }, [])

  const selectProject = useCallback((projectId: number) => {
    setProjects((current) => {
      const match = selectableProjects(current).find((project) => project.id === projectId)
      if (match == null) {
        return current
      }
      persistSelection(projectId)
      setActiveProjectId(projectId)
      return current
    })
  }, [])

  const value = useMemo<ActiveProjectContextValue>(() => {
    const selectable = selectableProjects(projects)
    return {
      projects,
      selectable,
      activeProjectId,
      activeProject: projects.find((project) => project.id === activeProjectId) ?? null,
      loading,
      error,
      selectProject,
      refreshProjects,
    }
  }, [projects, activeProjectId, loading, error, selectProject, refreshProjects])

  return <ActiveProjectContext.Provider value={value}>{children}</ActiveProjectContext.Provider>
}

export function useActiveProject(): ActiveProjectContextValue {
  const value = useContext(ActiveProjectContext)
  if (value == null) {
    throw new Error('useActiveProject must be used within ActiveProjectProvider')
  }
  return value
}
