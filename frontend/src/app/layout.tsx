
import type { Metadata } from "next";
import "./globals.css";



export const metadata: Metadata = {
  title: {
    default: "Timetable Management System | University of Ruhuna",
    template: "%s | University of Ruhuna",
  },
  description:
    "Timetable Management System for the Faculty of Engineering, University of Ruhuna, Sri Lanka.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="min-h-screen antialiased">{children}</body>
    </html>
  );
}
