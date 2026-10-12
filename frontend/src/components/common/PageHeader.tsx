type PageHeaderProps = {
  title: string
  description: string
}

export function PageHeader({ title, description }: PageHeaderProps) {
  return (
    <header className="border-b border-stone-200 pb-6">
      <h1 className="text-2xl font-semibold tracking-tight text-stone-950">{title}</h1>
      <p className="mt-2 max-w-2xl text-sm leading-6 text-stone-600">{description}</p>
    </header>
  )
}
