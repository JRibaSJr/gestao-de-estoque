import React from "react";
import "./App.css";
import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import { Toaster } from "sonner";
import { NotificationProvider } from "./contexts/NotificationContext";
import Dashboard from "./pages/Dashboard";
import Inventory from "./pages/Inventory";
import { Home, Package, Store, BarChart } from "lucide-react";

const Layout = ({ children }) => {
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Navigation */}
      <nav className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex">
              <div className="flex-shrink-0 flex items-center">
                <Package className="h-8 w-8 text-blue-600" />
                <span className="ml-2 text-xl font-bold text-gray-900">
                  Inventory Hub
                </span>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <Link
                to="/"
                className="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium flex items-center"
              >
                <Home className="h-4 w-4 mr-1" />
                Dashboard
              </Link>
              <Link
                to="/inventory"
                className="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium flex items-center"
              >
                <Package className="h-4 w-4 mr-1" />
                Inventário
              </Link>
              <Link
                to="/stores"
                className="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium flex items-center"
              >
                <Store className="h-4 w-4 mr-1" />
                Lojas
              </Link>
              <Link
                to="/reports"
                className="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium flex items-center"
              >
                <BarChart className="h-4 w-4 mr-1" />
                Relatórios
              </Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {children}
      </main>
    </div>
  );
};

function App() {
  return (
    <NotificationProvider>
      <div className="App">
        <BrowserRouter>
          <Layout>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/inventory" element={<Inventory />} />
              <Route path="/stores" element={<div className="p-6"><h1 className="text-2xl font-bold">Gestão de Lojas</h1><p>Em desenvolvimento...</p></div>} />
              <Route path="/reports" element={<div className="p-6"><h1 className="text-2xl font-bold">Relatórios</h1><p>Em desenvolvimento...</p></div>} />
            </Routes>
          </Layout>
        </BrowserRouter>
        <Toaster richColors position="top-right" />
      </div>
    </NotificationProvider>
  );
}

export default App;
