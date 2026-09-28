import { NavLink, Route, Routes } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import Candidatos from './pages/Candidatos'
import Eleitores from './pages/Eleitores'
import Votacao from './pages/Votacao'
import Resultado from './pages/Resultado'

const LINKS = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/votacao', label: 'Votação' },
  { to: '/resultado', label: 'Resultado' },
  { to: '/candidatos', label: 'Candidatos' },
  { to: '/eleitores', label: 'Eleitores' },
]

export default function App() {
  return (
    <>
      <header className="topbar">
        <div className="topbar-inner">
          <span className="brand">Urna simulada</span>
          <nav aria-label="Navegação principal">
            {LINKS.map((l) => (
              <NavLink key={l.to} to={l.to} end={l.end}>
                {l.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>
      <main className="page">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/votacao" element={<Votacao />} />
          <Route path="/resultado" element={<Resultado />} />
          <Route path="/candidatos" element={<Candidatos />} />
          <Route path="/eleitores" element={<Eleitores />} />
          <Route path="*" element={<p>Página não encontrada.</p>} />
        </Routes>
      </main>
    </>
  )
}
